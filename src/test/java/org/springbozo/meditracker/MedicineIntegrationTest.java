package org.springbozo.meditracker;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springbozo.meditracker.service.DailyMedService;
import org.springbozo.meditracker.service.MailService;
import org.springbozo.meditracker.service.MedicineSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** openFDA NDC import, medicine search, and the DailyMed label parser, against a real Postgres. */
@SpringBootTest
@AutoConfigureMockMvc
class MedicineIntegrationTest {

    static final EmbeddedPostgres pg;

    static {
        try {
            pg = EmbeddedPostgres.builder().start();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", () -> pg.getJdbcUrl("postgres", "postgres"));
        r.add("spring.datasource.username", () -> "postgres");
        r.add("spring.datasource.password", () -> "");
        r.add("jwt.secret", () -> "test-secret-test-secret-test-secret-0123456789");
        r.add("app.admin.email", () -> "boss@example.com");
        r.add("app.admin.password", () -> "admin-pass-123");
        r.add("app.medicine.auto-sync", () -> "false");
    }

    @AfterAll
    static void stop() throws IOException {
        pg.close();
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    MedicineSyncService sync;

    @MockitoBean
    MailService mailService;

    private static Map<String, Object> product(String ndc, String brand, String generic, String type,
                                               String category, boolean finished, List<Map<String, String>> ingredients) {
        return Map.of(
                "product_ndc", ndc, "brand_name", brand, "generic_name", generic,
                "labeler_name", "Test Pharma", "product_type", type, "marketing_category", category,
                "finished", finished, "dosage_form", "CAPSULE", "route", List.of("ORAL"),
                "active_ingredients", ingredients);
    }

    private Path ndcZip(List<Map<String, Object>> results) throws Exception {
        Path zip = Files.createTempFile("ndc-test", ".zip");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip))) {
            out.putNextEntry(new ZipEntry("drug-ndc-0001-of-0001.json"));
            out.write(json.writeValueAsBytes(Map.of("meta", Map.of(), "results", results)));
            out.closeEntry();
        }
        return zip;
    }

    private String adminToken() throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"boss@example.com\",\"password\":\"admin-pass-123\"}"))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }

    @Test
    void importsFinishedProductsWithStrengthsAndHidesDelisted() throws Exception {
        var prograf = product("0469-0607", "Prograf", "Tacrolimus", "HUMAN PRESCRIPTION DRUG", "NDA", true,
                List.of(Map.of("name", "TACROLIMUS", "strength", ".5 mg/1")));
        var myfortic = product("0078-0386", "Myfortic", "mycophenolic acid", "HUMAN PRESCRIPTION DRUG", "NDA", true,
                List.of(Map.of("name", "MYCOPHENOLIC ACID", "strength", "360 mg/1")));
        var combo = product("1111-2222", "Caduet", "amlodipine and atorvastatin", "HUMAN PRESCRIPTION DRUG", "NDA", true,
                List.of(Map.of("name", "AMLODIPINE", "strength", "5 mg/1"), Map.of("name", "ATORVASTATIN", "strength", "10 mg/1")));
        var bulk = product("3333-4444", "Bulk Tacrolimus", "tacrolimus", "BULK INGREDIENT", "BULK INGREDIENT", true, List.of());
        var homeopathic = product("5555-6666", "Arnica Prograf-ish", "arnica", "HUMAN OTC DRUG", "UNAPPROVED HOMEOPATHIC", true, List.of());
        var unfinished = product("7777-8888", "Unfinished Prograf", "tacrolimus", "HUMAN PRESCRIPTION DRUG", "NDA", false, List.of());

        sync.importFile(ndcZip(List.of(prograf, myfortic, combo, bulk, homeopathic, unfinished)));
        String token = adminToken();

        mvc.perform(get("/api/medicine/paged?q=prograf").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].strength").value("0.5 mg"))
                .andExpect(jsonPath("$.content[0].manufacturer").value("Test Pharma"))
                .andExpect(jsonPath("$.content[0].productNdc").value("0469-0607"));
        // search also matches the generic name, case-insensitively
        mvc.perform(get("/api/medicine/paged?q=MYCOPHEN").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.content[0].name").value("Myfortic"))
                .andExpect(jsonPath("$.content[0].strength").value("360 mg"));
        mvc.perform(get("/api/medicine/paged?q=caduet").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.content[0].strength").value("5 mg / 10 mg"));
        // % in a search is a literal character, not a wildcard
        mvc.perform(get("/api/medicine/paged?q=%25").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.totalElements").value(0));

        // next sync no longer lists Myfortic: it's hidden from search, but still loads by id for old prescriptions
        String found = mvc.perform(get("/api/medicine/paged?q=myfortic").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        int myforticId = json.readTree(found).at("/content/0/id").asInt();
        sync.importFile(ndcZip(List.of(prograf, combo)));
        mvc.perform(get("/api/medicine/paged?q=myfortic").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/medicine/" + myforticId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void syncStatusNeedsLogin() throws Exception {
        mvc.perform(get("/api/medicine/sync/status").header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.running").value(false));
        mvc.perform(get("/api/medicine/sync/status"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void labelImagesArePublicButOnlyForRealLabels() throws Exception {
        // no auth needed (img tags can't send it), and junk ids never reach DailyMed
        mvc.perform(get("/api/medicine-images/not-a-set-id/x.jpg")).andExpect(status().isNotFound());
        mvc.perform(get("/api/medicine-images/7f667de1-9dfa-4bd6-8ba0-15ee2d78873b/..%2F..%2Fetc.jpg"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void parsesPackageLabelImagesAndPillAppearance() throws Exception {
        // trimmed from the real Prograf label: product NDC on the inner manufacturedProduct,
        // pill characteristics on the outer one, package label section with LOINC 51945-4
        String xml = """
                <document xmlns="urn:hl7-org:v3" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                 <component><structuredBody>
                  <component><section>
                   <code code="48780-1"/>
                   <subject><manufacturedProduct>
                    <manufacturedProduct>
                     <code code="0469-0607"/><name>Prograf</name>
                     <asContent><containerPackagedProduct><code code="0469-0607-73"/></containerPackagedProduct>
                      <subjectOf><characteristic><code code="SPLCMBPRDTP"/><value displayName="IGNORED"/></characteristic></subjectOf>
                     </asContent>
                    </manufacturedProduct>
                    <subjectOf><characteristic><code code="SPLCOLOR"/><value displayName="YELLOW" xsi:type="CE"/></characteristic></subjectOf>
                    <subjectOf><characteristic><code code="SPLCOLOR"/><value displayName="WHITE" xsi:type="CE"/></characteristic></subjectOf>
                    <subjectOf><characteristic><code code="SPLSHAPE"/><value displayName="CAPSULE" xsi:type="CE"/></characteristic></subjectOf>
                    <subjectOf><characteristic><code code="SPLSIZE"/><value unit="mm" value="11" xsi:type="PQ"/></characteristic></subjectOf>
                    <subjectOf><characteristic><code code="SPLIMPRINT"/><value xsi:type="ST">f;607;05;mg</value></characteristic></subjectOf>
                   </manufacturedProduct></subject>
                   <subject><manufacturedProduct>
                    <manufacturedProduct><code code="0469-0617"/><name>Prograf</name></manufacturedProduct>
                   </manufacturedProduct></subject>
                  </section></component>
                  <component><section>
                   <code code="51945-4"/><text>NDC 0469-0617-73 Prograf 1 mg 100 capsules</text>
                   <component><observationMedia><value><reference value="one-mg-carton.jpg"/></value></observationMedia></component>
                  </section></component>
                  <component><section>
                   <code code="34067-9"/>
                   <component><observationMedia><value><reference value="instructions-01.jpg"/></value></observationMedia></component>
                  </section></component>
                  <component><section>
                   <code code="51945-4"/><text>NDC 0469-0607-73 Prograf 0.5 mg 100 capsules</text>
                   <component><observationMedia><value><reference value="prograf-carton.jpg"/></value></observationMedia></component>
                  </section></component>
                 </structuredBody></component>
                </document>
                """;
        var label = DailyMedService.parse("7f667de1-9dfa-4bd6-8ba0-15ee2d78873b", xml.getBytes(StandardCharsets.UTF_8));

        // only package-panel images, never the instructions drawing; each product's own carton first
        assertThat(label.packageImages()).containsExactly("one-mg-carton.jpg", "prograf-carton.jpg");
        assertThat(label.imagesFor("0469-0607", "0.5 mg")).containsExactly("prograf-carton.jpg", "one-mg-carton.jpg");
        assertThat(label.imagesFor("0469-0617", "1 mg")).containsExactly("one-mg-carton.jpg", "prograf-carton.jpg");

        var a = label.appearanceByNdc().get("0469-0607");
        assertThat(a.color()).isEqualTo("Yellow / White");
        assertThat(a.shape()).isEqualTo("Capsule");
        assertThat(a.size()).isEqualTo("11 mm");
        assertThat(a.imprint()).isEqualTo("f 607 05 mg");
        // the second product has no characteristics on the label
        assertThat(label.appearanceByNdc()).doesNotContainKey("0469-0617");
    }

    @Test
    void ranksPanelsByStrengthWhenTheyDontPrintTheNdc() {
        // real Prograf panels are headed "0.5 mg Individual Carton" etc. with no NDC in the text
        var label = new DailyMedService.Label("7f667de1-9dfa-4bd6-8ba0-15ee2d78873b",
                List.of("a.jpg", "b.jpg", "c.jpg"),
                List.of(new DailyMedService.Panel(List.of("a.jpg"), "PRINCIPAL DISPLAY PANEL 0.5 mg Individual Carton"),
                        new DailyMedService.Panel(List.of("b.jpg"), "PRINCIPAL DISPLAY PANEL 1 mg Individual Carton"),
                        new DailyMedService.Panel(List.of("c.jpg"), "PRINCIPAL DISPLAY PANEL 5 mg Individual Carton")),
                Map.of(), java.time.Instant.now());

        assertThat(label.imagesFor("0469-0657", "5 mg")).startsWith("c.jpg"); // not fooled by "0.5 mg"
        assertThat(label.imagesFor("0469-0617", "1 mg")).startsWith("b.jpg");
        assertThat(label.imagesFor("0469-0607", "0.5 mg")).startsWith("a.jpg");
        assertThat(label.imagesFor("9999-9999", "250 mg")).containsExactly("a.jpg", "b.jpg", "c.jpg");
    }

    @Test
    void labelParserRejectsDoctypes() {
        String xxe = "<?xml version=\"1.0\"?><!DOCTYPE d [<!ENTITY x SYSTEM \"file:///etc/passwd\">]><document>&x;</document>";
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class,
                () -> DailyMedService.parse("7f667de1-9dfa-4bd6-8ba0-15ee2d78873b", xxe.getBytes(StandardCharsets.UTF_8)));
    }
}
