package org.springbozo.meditracker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Reads a drug's official label (SPL) from DailyMed: the package label images and, for each
 * product, the pill description (colour, shape, size, imprint) so users can check a pill by eye.
 * There is no free source of actual pill photos since NLM retired Pillbox/RxImage in 2021.
 */
@Slf4j
@Service
public class DailyMedService {

    private static final String BASE = "https://dailymed.nlm.nih.gov/dailymed";
    public static final Pattern SET_ID = Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
    public static final Pattern IMAGE_NAME = Pattern.compile("^[A-Za-z0-9._-]{1,120}\\.(jpg|jpeg|png|gif)$", Pattern.CASE_INSENSITIVE);
    // LOINC code of the "PACKAGE LABEL.PRINCIPAL DISPLAY PANEL" section
    private static final String PACKAGE_LABEL_SECTION = "51945-4";
    private static final Duration TTL = Duration.ofDays(7);
    private static final int MAX_CACHED = 2000;

    public record Appearance(String color, String shape, String size, String imprint, String score) {}

    /** One package label panel: its images and its text (heading, sometimes the package NDC). */
    public record Panel(List<String> images, String text) {}

    public record Label(String setId, List<String> packageImages, List<Panel> panels,
                        Map<String, Appearance> appearanceByNdc, Instant fetchedAt) {

        /**
         * All package images, best match for this product first. One label covers every strength and form,
         * and panels either print the package NDC ("NDC 0469-0607-73") or are headed by strength ("0.5 mg Carton").
         */
        public List<String> imagesFor(String productNdc, String strength) {
            Pattern strengthWord = strength == null ? null
                    : Pattern.compile("(?<![\\d.])" + Pattern.quote(strength.split(" / ")[0]) + "\\b", Pattern.CASE_INSENSITIVE);
            List<Panel> ranked = new ArrayList<>(panels);
            ranked.sort(Comparator.comparingInt((Panel p) -> {
                if (productNdc != null && p.text().contains(productNdc)) return 0;
                if (strengthWord != null && strengthWord.matcher(p.text()).find()) return 1;
                return 2;
            }));
            Set<String> out = new LinkedHashSet<>();
            ranked.forEach(p -> out.addAll(p.images()));
            return List.copyOf(out);
        }
    }

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final Map<String, Label> cache = new ConcurrentHashMap<>();

    /** Empty if the label can't be fetched; the details page then just shows the text data. */
    public Optional<Label> label(String setId) {
        if (setId == null || !SET_ID.matcher(setId).matches()) return Optional.empty();
        Label cached = cache.get(setId);
        if (cached != null && cached.fetchedAt().plus(TTL).isAfter(Instant.now())) return Optional.of(cached);
        try {
            HttpResponse<byte[]> res = http.send(
                    HttpRequest.newBuilder(URI.create(BASE + "/services/v2/spls/" + setId + ".xml"))
                            .timeout(Duration.ofSeconds(20)).build(),
                    HttpResponse.BodyHandlers.ofByteArray());
            if (res.statusCode() != 200) return Optional.ofNullable(cached);
            Label label = parse(setId, res.body());
            if (cache.size() >= MAX_CACHED) cache.clear();
            cache.put(setId, label);
            return Optional.of(label);
        } catch (Exception e) {
            log.warn("DailyMed label {} unavailable: {}", setId, e.toString());
            return Optional.ofNullable(cached);
        }
    }

    /** Image bytes, only for images the label itself lists, so this can't be used to fetch anything else. */
    public Optional<HttpResponse<byte[]>> image(String setId, String name) {
        if (name == null || !IMAGE_NAME.matcher(name).matches()) return Optional.empty();
        Optional<Label> label = label(setId);
        if (label.isEmpty() || !label.get().packageImages().contains(name)) return Optional.empty();
        try {
            String url = BASE + "/image.cfm?setid=" + setId + "&name=" + URLEncoder.encode(name, StandardCharsets.UTF_8);
            HttpResponse<byte[]> res = http.send(
                    HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20)).build(),
                    HttpResponse.BodyHandlers.ofByteArray());
            return res.statusCode() == 200 ? Optional.of(res) : Optional.empty();
        } catch (Exception e) {
            log.warn("DailyMed image {}/{} unavailable: {}", setId, name, e.toString());
            return Optional.empty();
        }
    }

    public static Label parse(String setId, byte[] xml) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        // untrusted XML: no DTDs or external entities (XXE)
        f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        f.setFeature("http://xml.org/sax/features/external-general-entities", false);
        f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        f.setXIncludeAware(false);
        f.setExpandEntityReferences(false);
        f.setNamespaceAware(true);
        Document doc = f.newDocumentBuilder().parse(new ByteArrayInputStream(xml));

                Map<String, Appearance> appearance = new LinkedHashMap<>();
        // SPL nests subject/manufacturedProduct/manufacturedProduct: the NDC code is on the inner
        // element, the pill characteristics on the outer one's own subjectOf children
        NodeList products = doc.getElementsByTagNameNS("*", "manufacturedProduct");
        for (int i = 0; i < products.getLength(); i++) {
            Element product = (Element) products.item(i);
            Element code = firstChild(product, "code");
            if (code == null || code.getAttribute("code").isBlank()) continue;
            if (!(product.getParentNode() instanceof Element outer) || !"manufacturedProduct".equals(outer.getLocalName())) continue;
            String ndc = code.getAttribute("code");
            Map<String, String> c = characteristics(outer);
            if (c.isEmpty()) continue;
            appearance.putIfAbsent(ndc, new Appearance(
                    c.get("SPLCOLOR"), c.get("SPLSHAPE"), c.get("SPLSIZE"), c.get("SPLIMPRINT"), c.get("SPLSCORE")));
        }

        Set<String> images = new LinkedHashSet<>();
        List<Panel> panels = new ArrayList<>();
        NodeList sections = doc.getElementsByTagNameNS("*", "section");
        for (int i = 0; i < sections.getLength(); i++) {
            Element section = (Element) sections.item(i);
            Element code = firstChild(section, "code");
            if (code == null || !PACKAGE_LABEL_SECTION.equals(code.getAttribute("code"))) continue;
            List<String> panelImages = new ArrayList<>();
            NodeList refs = section.getElementsByTagNameNS("*", "reference");
            for (int j = 0; j < refs.getLength(); j++) {
                String name = ((Element) refs.item(j)).getAttribute("value");
                if (IMAGE_NAME.matcher(name).matches() && !panelImages.contains(name)) panelImages.add(name);
            }
            if (panelImages.isEmpty()) continue;
            images.addAll(panelImages);
            // keep only what ranking needs; full section text can be long
            String text = section.getTextContent().replaceAll("\\s+", " ").trim();
            panels.add(new Panel(List.copyOf(panelImages), text.substring(0, Math.min(400, text.length()))));
        }
        return new Label(setId, List.copyOf(images), List.copyOf(panels), appearance, Instant.now());
    }

    private static Map<String, String> characteristics(Element product) {
        Map<String, String> out = new HashMap<>();
        for (var n = product.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (!(n instanceof Element subjectOf) || !"subjectOf".equals(subjectOf.getLocalName())) continue;
            Element ch = firstChild(subjectOf, "characteristic");
            if (ch == null) continue;
            Element code = firstChild(ch, "code");
            Element value = firstChild(ch, "value");
            if (code == null || value == null) continue;
            String key = code.getAttribute("code");
            String text = switch (key) {
                case "SPLCOLOR", "SPLSHAPE" -> titleCase(value.getAttribute("displayName"));
                case "SPLSIZE" -> value.getAttribute("value").isBlank() ? null
                        : value.getAttribute("value") + " " + value.getAttribute("unit");
                case "SPLIMPRINT" -> value.getTextContent().trim().replace(';', ' ');
                case "SPLSCORE" -> value.getAttribute("value");
                default -> null;
            };
            if (text == null || text.isBlank()) continue;
            // a two-tone capsule lists SPLCOLOR twice
            out.merge(key, text.trim(), (a, b) -> a.equals(b) ? a : a + " / " + b);
        }
        return out;
    }

    private static Element firstChild(Element parent, String localName) {
        for (var n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e && localName.equals(e.getLocalName())) return e;
        }
        return null;
    }

    private static String titleCase(String s) {
        if (s == null || s.isBlank()) return null;
        return s.charAt(0) + s.substring(1).toLowerCase();
    }
}
