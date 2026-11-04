package org.springbozo.meditracker.service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipInputStream;

/**
 * Keeps the medicine table in step with the openFDA NDC directory (every US-listed drug product).
 * Runs on first start when the table is empty, weekly after that, and on demand from the admin page.
 */
@Slf4j
@Service
public class MedicineSyncService {

    public static final String NDC_URL = "https://download.open.fda.gov/drug/ndc/drug-ndc-0001-of-0001.json.zip";

    // finished products people actually take; skips bulk ingredients, allergen extracts etc.
    private static final Set<String> PRODUCT_TYPES = Set.of(
            "HUMAN PRESCRIPTION DRUG", "HUMAN OTC DRUG", "VACCINE", "PLASMA DERIVATIVE", "CELLULAR THERAPY");
    private static final List<String> OPENFDA_KEYS = List.of("rxcui", "substance_name", "pharm_class_epc", "pharm_class_moa");
    private static final int BATCH = 1000;

    private static final String UPSERT = """
            INSERT INTO medicine (product_ndc, medicine_name, generic_name, manufacturer, strength, dosage_form,
                                  route, product_type, spl_set_id, openfda, active, last_synced_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, TRUE, ?)
            ON CONFLICT (product_ndc) DO UPDATE SET
                medicine_name = EXCLUDED.medicine_name, generic_name = EXCLUDED.generic_name,
                manufacturer = EXCLUDED.manufacturer, strength = EXCLUDED.strength,
                dosage_form = EXCLUDED.dosage_form, route = EXCLUDED.route,
                product_type = EXCLUDED.product_type, spl_set_id = EXCLUDED.spl_set_id,
                openfda = EXCLUDED.openfda, active = TRUE, last_synced_at = EXCLUDED.last_synced_at
            """;

    public record Status(boolean running, Instant startedAt, Instant finishedAt, int imported, String error) {}

    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final boolean autoSync;
    private final String sourceUrl;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile Status status = new Status(false, null, null, 0, null);

    public MedicineSyncService(JdbcTemplate jdbc, ObjectMapper json,
                               @Value("${app.medicine.auto-sync:true}") boolean autoSync,
                               @Value("${app.medicine.ndc-url:" + NDC_URL + "}") String sourceUrl) {
        this.jdbc = jdbc;
        this.json = json;
        this.autoSync = autoSync;
        this.sourceUrl = sourceUrl;
    }

    public Status getStatus() {
        return status;
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void syncIfEmpty() {
        Integer synced = jdbc.queryForObject("SELECT count(*) FROM medicine WHERE product_ndc IS NOT NULL", Integer.class);
        if (autoSync && synced != null && synced == 0) {
            log.info("Medicine list is empty, importing from openFDA");
            sync();
        }
    }

    // Sundays 04:00 server time; openFDA refreshes the file daily
    @Async
    @Scheduled(cron = "${app.medicine.sync-cron:0 0 4 * * SUN}")
    public void scheduledSync() {
        if (autoSync) sync();
    }

    /** Starts a sync in the background. Returns false if one is already running. */
    @Async
    public void syncAsync() {
        sync();
    }

    public boolean isRunning() {
        return running.get();
    }

    public void sync() {
        Path zip = null;
        try {
            zip = Files.createTempFile("openfda-ndc", ".zip");
            Path target = zip;
            importFile(() -> download(target), zip);
        } catch (Exception e) {
            log.error("openFDA sync could not start", e);
        } finally {
            if (zip != null) {
                try { Files.deleteIfExists(zip); } catch (Exception ignored) {}
            }
        }
    }

    interface Step { void run() throws Exception; }

    /** Imports an already-downloaded NDC zip. */
    public void importFile(Path zip) {
        importFile(() -> {}, zip);
    }

    private void importFile(Step fetch, Path zip) {
        if (!running.compareAndSet(false, true)) return;
        Instant started = Instant.now();
        status = new Status(true, started, null, 0, null);
        try {
            fetch.run();
            int imported = load(zip, Timestamp.from(started));
            // products FDA no longer lists are hidden, not deleted, so old prescriptions keep working
            int hidden = jdbc.update("UPDATE medicine SET active = FALSE WHERE product_ndc IS NOT NULL AND active AND last_synced_at < ?",
                    Timestamp.from(started));
            log.info("openFDA sync done: {} products, {} no longer listed", imported, hidden);
            status = new Status(false, started, Instant.now(), imported, null);
        } catch (Exception e) {
            log.error("openFDA sync failed", e);
            status = new Status(false, started, Instant.now(), status.imported(), "Sync failed, see the server log");
        } finally {
            running.set(false);
        }
    }

    private void download(Path target) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        HttpResponse<Path> res = client.send(
                HttpRequest.newBuilder(URI.create(sourceUrl)).timeout(Duration.ofMinutes(10)).build(),
                HttpResponse.BodyHandlers.ofFile(target));
        if (res.statusCode() != 200) {
            throw new IllegalStateException("openFDA download returned HTTP " + res.statusCode());
        }
    }

    // streams the JSON so the ~300 MB file never sits in memory
    int load(Path zip, Timestamp syncedAt) throws Exception {
        int imported = 0;
        List<Object[]> batch = new ArrayList<>(BATCH);
        try (InputStream in = Files.newInputStream(zip); ZipInputStream zin = new ZipInputStream(in)) {
            if (zin.getNextEntry() == null) throw new IllegalStateException("openFDA zip is empty");
            try (JsonParser p = json.getFactory().createParser(zin)) {
                while (p.nextToken() != null) {
                    if (p.currentToken() == JsonToken.FIELD_NAME && "results".equals(p.currentName())) {
                        p.nextToken(); // START_ARRAY
                        while (p.nextToken() == JsonToken.START_OBJECT) {
                            Object[] row = toRow(json.readTree(p), syncedAt);
                            if (row == null) continue;
                            batch.add(row);
                            if (batch.size() == BATCH) {
                                imported += flush(batch);
                                status = new Status(true, status.startedAt(), null, imported, null);
                            }
                        }
                    }
                }
            }
        }
        return imported + flush(batch);
    }

    private int flush(List<Object[]> batch) {
        if (batch.isEmpty()) return 0;
        jdbc.batchUpdate(UPSERT, batch);
        int n = batch.size();
        batch.clear();
        return n;
    }

    Object[] toRow(JsonNode r, Timestamp syncedAt) throws Exception {
        String ndc = text(r, "product_ndc");
        String type = text(r, "product_type");
        String category = text(r, "marketing_category");
        if (ndc == null || type == null || !PRODUCT_TYPES.contains(type)) return null;
        if (r.path("finished").isBoolean() && !r.path("finished").asBoolean()) return null;
        if (category != null && category.contains("HOMEOPATHIC")) return null;

        String generic = text(r, "generic_name");
        String name = Optional.ofNullable(text(r, "brand_name")).orElse(generic);
        if (name == null) return null;

        JsonNode fda = r.path("openfda");
        Map<String, List<String>> openfda = new LinkedHashMap<>();
        for (String k : OPENFDA_KEYS) {
            List<String> values = new ArrayList<>();
            fda.path(k).forEach(v -> values.add(v.asText()));
            if (!values.isEmpty()) openfda.put(k, values);
        }
        String setId = fda.path("spl_set_id").path(0).asText(null);

        List<String> routes = new ArrayList<>();
        r.path("route").forEach(v -> routes.add(v.asText()));

        return new Object[]{
                cut(ndc, 20), cut(name, 255), cut(generic, 1000), cut(text(r, "labeler_name"), 500),
                cut(strength(r.path("active_ingredients")), 500), cut(text(r, "dosage_form"), 150),
                cut(routes.isEmpty() ? null : String.join(", ", routes), 150), cut(type, 60), cut(setId, 40),
                openfda.isEmpty() ? null : json.writeValueAsString(openfda), syncedAt
        };
    }

    /** "1 mg/1" becomes "1 mg", ".5 mg/1" becomes "0.5 mg"; combinations are joined with " / ". */
    public static String strength(JsonNode ingredients) {
        List<String> parts = new ArrayList<>();
        ingredients.forEach(i -> {
            String s = i.path("strength").asText("").trim();
            if (s.isEmpty()) return;
            s = s.replaceAll("/1$", "").replaceAll("(^|\\s)\\.(\\d)", "$10.$2");
            parts.add(s);
        });
        return parts.isEmpty() ? null : String.join(" / ", parts);
    }

    private static String text(JsonNode n, String field) {
        JsonNode v = n.path(field);
        if (v.isMissingNode() || v.isNull()) return null;
        String s = v.asText().trim();
        return s.isEmpty() ? null : s;
    }

    private static String cut(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}
