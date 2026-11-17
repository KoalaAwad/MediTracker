package org.springbozo.meditracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springbozo.meditracker.model.*;
import org.springbozo.meditracker.model.embedded.ScheduleEntry;
import org.springbozo.meditracker.model.enums.DoseStatus;
import org.springbozo.meditracker.repository.DoseLogRepository;
import org.springbozo.meditracker.repository.PrescriptionRepository;
import org.springbozo.meditracker.repository.PushSubscriptionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Sends a push reminder when a dose is due, and one follow-up if it still isn't logged 30 minutes later.
 * Runs every minute; the unique (prescription, time) row makes each reminder go out once even if a tick
 * repeats or the server restarts.
 */
@Slf4j
@Service
public class ReminderService {

    // reminders more than this late (server was down) are skipped rather than sent stale
    static final Duration CATCH_UP = Duration.ofMinutes(10);
    static final Duration FOLLOW_UP_AFTER = Duration.ofMinutes(30);
    // after this a dose counts as missed and gets no more notifications
    static final Duration MISSED_AFTER = Duration.ofHours(2);
    private static final Duration ACTION_TOKEN_VALID = Duration.ofHours(24);
    // 12-hour clock, e.g. "8:00 AM"
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.US);

    public record Due(Prescription prescription, Instant scheduledAt) {}

    private final PrescriptionRepository prescriptions;
    private final DoseLogRepository doses;
    private final PushSubscriptionRepository subscriptions;
    private final WebPushService push;
    private final ObjectMapper json;
    private final TransactionTemplate tx;
    private final boolean enabled;
    private final byte[] actionKey;

    public ReminderService(PrescriptionRepository prescriptions, DoseLogRepository doses,
                           PushSubscriptionRepository subscriptions, WebPushService push, ObjectMapper json,
                           TransactionTemplate tx,
                           @Value("${app.reminders.enabled:true}") boolean enabled,
                           @Value("${jwt.secret}") String jwtSecret) throws Exception {
        this.prescriptions = prescriptions;
        this.doses = doses;
        this.subscriptions = subscriptions;
        this.push = push;
        this.json = json;
        this.tx = tx;
        this.enabled = enabled;
        // separate key for the notification buttons, derived so there's no extra secret to configure
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        this.actionKey = mac.doFinal("dose-action".getBytes(StandardCharsets.UTF_8));
    }

    // ---- schedule maths ----

    static ZoneId zoneOf(Prescription p) {
        try {
            return ZoneId.of(p.getTimeZone());
        } catch (Exception e) {
            return ZoneOffset.UTC;
        }
    }

    /** The prescription's doses on one local date, in time order. */
    public static List<Instant> occurrencesOn(Prescription p, LocalDate date) {
        if (!p.isActive() || date.isBefore(p.getStartDate()) || (p.getEndDate() != null && date.isAfter(p.getEndDate()))) {
            return List.of();
        }
        ZoneId zone = zoneOf(p);
        return p.getSchedule().stream()
                .filter(e -> e.getDayOfWeek() == date.getDayOfWeek())
                .map(ScheduleEntry::getTimeOfDay)
                .distinct()
                // ZonedDateTime moves a time inside a DST gap forward, so the dose still happens
                .map(t -> ZonedDateTime.of(date, t, zone).toInstant())
                .sorted()
                .toList();
    }

    /** Doses with from < time <= to. */
    static List<Instant> occurrencesBetween(Prescription p, Instant from, Instant to) {
        ZoneId zone = zoneOf(p);
        LocalDate first = from.atZone(zone).toLocalDate();
        LocalDate last = to.atZone(zone).toLocalDate();
        List<Instant> out = new ArrayList<>();
        for (LocalDate d = first; !d.isAfter(last); d = d.plusDays(1)) {
            for (Instant i : occurrencesOn(p, d)) {
                if (i.isAfter(from) && !i.isAfter(to)) out.add(i);
            }
        }
        return out;
    }

    // ---- the job ----

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void scheduledTick() {
        if (!enabled) return;
        try {
            tick(Instant.now());
        } catch (Exception e) {
            log.error("Reminder tick failed", e);
        }
    }

    public void tick(Instant now) {
        List<Due> due = tx.execute(s -> {
            List<Due> list = new ArrayList<>();
            for (Prescription p : prescriptions.findActiveWithSchedule()) {
                for (Instant at : occurrencesBetween(p, now.minus(CATCH_UP), now)) list.add(new Due(p, at));
            }
            return list;
        });
        for (Due d : Objects.requireNonNull(due)) remind(d, now);

        List<DoseLog> followUps = tx.execute(s -> doses.findNeedingFollowUp(
                DoseStatus.PENDING, now.minus(FOLLOW_UP_AFTER), now.minus(MISSED_AFTER)));
        for (DoseLog d : Objects.requireNonNull(followUps)) {
            d.setFollowedUpAt(now);
            doses.save(d);
            sendDose(d, d.getPrescription(), true);
        }
    }

    private void remind(Due due, Instant now) {
        Optional<DoseLog> existing = doses.findOne(due.prescription().getId(), due.scheduledAt());
        // already reminded, or the user logged it early
        if (existing.isPresent()) return;
        DoseLog dose = new DoseLog();
        dose.setPrescription(due.prescription());
        dose.setScheduledAt(due.scheduledAt());
        dose.setStatus(DoseStatus.PENDING);
        dose.setNotifiedAt(now);
        try {
            dose = doses.saveAndFlush(dose);
        } catch (DataIntegrityViolationException raced) {
            return; // another tick got there first
        }
        sendDose(dose, due.prescription(), false);
    }

    private void sendDose(DoseLog dose, Prescription p, boolean followUp) {
        String name = medicineLabel(p.getMedicine());
        String time = dose.getScheduledAt().atZone(zoneOf(p)).format(HH_MM);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", followUp ? "Did you take " + name + "?" : "Time for " + name);
        payload.put("body", withNotes("Take " + dosageLabel(p) + " · " + time, p.getNotes()));
        payload.put("tag", "dose-" + dose.getId());
        payload.put("doseId", dose.getId());
        payload.put("token", actionToken(dose.getId(), dose.getScheduledAt()));
        payload.put("url", "/dashboard");
        sendToUser(p.getPatient().getUser(), payload);
    }

    /** Sends to every device of the user; drops subscriptions the push service says are dead. Returns devices reached. */
    public int sendToUser(User user, Map<String, Object> payload) {
        String body;
        try {
            body = json.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        int sent = 0;
        for (PushSubscription s : subscriptions.findByUser(user.getId())) {
            switch (push.send(s.getEndpoint(), s.getP256dh(), s.getAuth(), body)) {
                case SENT -> {
                    sent++;
                    s.setLastSuccessAt(Instant.now());
                    subscriptions.save(s);
                }
                case GONE -> subscriptions.delete(s);
                case FAILED -> { }
            }
        }
        return sent;
    }

    // ---- labels ----

    // notifications show about two lines, so long notes are cut
    static String withNotes(String body, String notes) {
        if (notes == null || notes.isBlank()) return body;
        String n = notes.strip().replaceAll("\\s+", " ");
        return body + "\n" + (n.length() > 120 ? n.substring(0, 119) + "…" : n);
    }

    public static String medicineLabel(Medicine m) {
        return m.getStrength() == null ? m.getName() : m.getName() + " " + m.getStrength();
    }

    /** "1 capsule", "2 tablets", "0.5 ml" */
    public static String dosageLabel(Prescription p) {
        BigDecimal amount = p.getDosage().getAmount().stripTrailingZeros();
        String unit = p.getDosage().getUnit().name().toLowerCase();
        boolean countable = !(unit.equals("ml") || unit.equals("mg") || unit.equals("mcg"));
        if (countable && amount.compareTo(BigDecimal.ONE) != 0) unit = unit.endsWith("ch") ? unit + "es" : unit + "s";
        return amount.toPlainString() + " " + unit;
    }

    // ---- notification button tokens ----

    /** Lets the service worker mark a dose without a login token; only valid for that dose, for 24 hours. */
    public String actionToken(int doseId, Instant scheduledAt) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(actionKey, "HmacSHA256"));
            byte[] sig = mac.doFinal((doseId + ":" + scheduledAt.getEpochSecond()).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(sig);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean verifyActionToken(DoseLog dose, String token, Instant now) {
        if (token == null || now.isAfter(dose.getScheduledAt().plus(ACTION_TOKEN_VALID))) return false;
        byte[] expected = actionToken(dose.getId(), dose.getScheduledAt()).getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(expected, token.getBytes(StandardCharsets.US_ASCII));
    }
}
