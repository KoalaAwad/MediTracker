package org.springbozo.meditracker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springbozo.meditracker.controller.DoseController;
import org.springbozo.meditracker.model.Prescription;
import org.springbozo.meditracker.model.embedded.Dosage;
import org.springbozo.meditracker.model.embedded.ScheduleEntry;
import org.springbozo.meditracker.model.enums.DosageUnit;
import org.springbozo.meditracker.service.MailService;
import org.springbozo.meditracker.service.ReminderService;
import org.springbozo.meditracker.service.WebPushService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Dose reminders end to end: subscribe, due reminder, follow-up, logging, notification buttons. */
@SpringBootTest
@AutoConfigureMockMvc
class ReminderIntegrationTest {

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
        // the test drives tick() itself
        r.add("app.reminders.enabled", () -> "false");
    }

    @AfterAll
    static void stop() throws IOException {
        pg.close();
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired ReminderService reminders;
    @Autowired org.springbozo.meditracker.config.RateLimiter rateLimiter;
    @MockitoBean MailService mailService;
    @MockitoBean WebPushService push;

    static final ZoneId DUBAI = ZoneId.of("Asia/Dubai");
    static final String ENDPOINT = "https://fcm.googleapis.com/fcm/send/test-device";

    String token;
    int prescriptionId;
    Instant eightAm;

    @BeforeEach
    void setUp() throws Exception {
        when(push.send(anyString(), anyString(), anyString(), anyString())).thenReturn(WebPushService.Result.SENT);
        rateLimiter.reset();
        // only this test's prescription should fire
        jdbc.update("UPDATE prescription SET active = FALSE");
        String email = "r" + System.nanoTime() + "@example.com";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", "remind-me-1", "name", "R"))));
        jdbc.update("UPDATE users SET email_verified = TRUE WHERE email = ?", email);
        token = json.readTree(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", "remind-me-1"))))
                .andReturn().getResponse().getContentAsString()).get("token").asText();

        Integer medicineId = jdbc.queryForObject("""
                INSERT INTO medicine (medicine_name, generic_name, strength, active) VALUES ('Prograf', 'tacrolimus', '1 mg', TRUE)
                RETURNING medicine_id""", Integer.class);
        // every day at 08:00 Dubai time, starting yesterday
        LocalDate today = LocalDate.now(DUBAI);
        List<Map<String, String>> schedule = java.util.Arrays.stream(DayOfWeek.values())
                .map(d -> Map.of("dayOfWeek", d.name(), "timeOfDay", "08:00")).toList();
        String created = mvc.perform(post("/api/prescriptions").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "medicineId", medicineId,
                                "dosage", Map.of("amount", 1, "unit", "CAPSULE"),
                                "startDate", today.minusDays(1).toString(),
                                "timeZone", "Asia/Dubai",
                                "schedule", schedule))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        prescriptionId = json.readTree(created).get("prescriptionId").asInt();
        eightAm = ZonedDateTime.of(today, LocalTime.of(8, 0), DUBAI).toInstant();

        mvc.perform(post("/api/push/subscriptions").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("endpoint", ENDPOINT + System.nanoTime(),
                                "keys", Map.of("p256dh", "BPub", "auth", "authsecret")))))
                .andExpect(status().isOk());
        clearInvocations(push);
    }

    private List<JsonNode> sentPayloads() throws Exception {
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(push, atLeast(0)).send(anyString(), anyString(), anyString(), body.capture());
        List<JsonNode> out = new java.util.ArrayList<>();
        for (String b : body.getAllValues()) out.add(json.readTree(b));
        return out;
    }

    @Test
    void remindsOnceThenFollowsUpOnce() throws Exception {
        reminders.tick(eightAm.minusSeconds(60));
        assertThat(sentPayloads()).as("not due yet").isEmpty();

        reminders.tick(eightAm.plusSeconds(30));
        reminders.tick(eightAm.plusSeconds(90)); // next minute: no duplicate
        List<JsonNode> sent = sentPayloads();
        assertThat(sent).hasSize(1);
        assertThat(sent.get(0).get("title").asText()).isEqualTo("Time for Prograf 1 mg");
        assertThat(sent.get(0).get("body").asText()).isEqualTo("Take 1 capsule · 8:00 AM");

        reminders.tick(eightAm.plus(Duration.ofMinutes(31)));
        reminders.tick(eightAm.plus(Duration.ofMinutes(32)));
        sent = sentPayloads();
        assertThat(sent).hasSize(2);
        assertThat(sent.get(1).get("title").asText()).isEqualTo("Did you take Prograf 1 mg?");
    }

    @Test
    void noFollowUpOnceTaken() throws Exception {
        reminders.tick(eightAm.plusSeconds(30));
        JsonNode reminder = sentPayloads().get(0);

        // the "Taken" button on the notification
        mvc.perform(post("/api/doses/quick").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("doseId", reminder.get("doseId").asInt(),
                                "token", reminder.get("token").asText(), "status", "TAKEN"))))
                .andExpect(status().isOk());

        reminders.tick(eightAm.plus(Duration.ofMinutes(31)));
        assertThat(sentPayloads()).hasSize(1);
        mvc.perform(get("/api/doses/today").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$[0].status").value("TAKEN"))
                .andExpect(jsonPath("$[0].localTime").value("8:00 AM"))
                .andExpect(jsonPath("$[0].dosage").value("1 capsule"));
    }

    private Map<String, Object> prescriptionWithNotes(String notes) {
        Integer medicineId = jdbc.queryForObject("SELECT medicine_id FROM prescription WHERE prescription_id = ?",
                Integer.class, prescriptionId);
        List<Map<String, String>> schedule = java.util.Arrays.stream(DayOfWeek.values())
                .map(d -> Map.of("dayOfWeek", d.name(), "timeOfDay", "08:00")).toList();
        return Map.of("medicineId", medicineId, "dosage", Map.of("amount", 2, "unit", "TABLET"),
                "startDate", LocalDate.now(DUBAI).minusDays(1).toString(), "timeZone", "Asia/Dubai",
                "schedule", schedule, "notes", notes);
    }

    @Test
    void notesAreSavedShownAndIncludedInReminders() throws Exception {
        jdbc.update("UPDATE prescription SET active = FALSE");
        mvc.perform(post("/api/prescriptions").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(prescriptionWithNotes("  Take with food,\n an hour after waking  "))))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/prescriptions/me").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$[0].notes").value("Take with food,\n an hour after waking"));

        reminders.tick(eightAm.plusSeconds(30));
        assertThat(sentPayloads().get(0).get("body").asText())
                .isEqualTo("Take 2 tablets · 8:00 AM\nTake with food, an hour after waking");
    }

    @Test
    void notesOver500CharactersAreRejected() throws Exception {
        mvc.perform(post("/api/prescriptions").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(prescriptionWithNotes("x".repeat(501)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Notes must be 500 characters or less"));
    }

    @Test
    void noFollowUpAfterThePrescriptionIsDeleted() throws Exception {
        reminders.tick(eightAm.plusSeconds(30));
        mvc.perform(delete("/api/prescriptions/" + prescriptionId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        reminders.tick(eightAm.plus(Duration.ofMinutes(31)));
        assertThat(sentPayloads()).hasSize(1);
    }

    @Test
    void doseStatusIsNowFromHalfAnHourBeforeToAnHourAfter() {
        Instant at = eightAm;
        assertThat(DoseController.status(null, at, at.minus(Duration.ofMinutes(31)))).isEqualTo("UPCOMING");
        assertThat(DoseController.status(null, at, at.minus(Duration.ofMinutes(30)))).isEqualTo("NOW");
        assertThat(DoseController.status(null, at, at)).isEqualTo("NOW");
        assertThat(DoseController.status(null, at, at.plus(Duration.ofMinutes(60)))).isEqualTo("NOW");
        assertThat(DoseController.status(null, at, at.plus(Duration.ofMinutes(61)))).isEqualTo("PAST");

        // a logged dose keeps its status wherever it sits in the day
        var taken = new org.springbozo.meditracker.model.DoseLog();
        taken.setStatus(org.springbozo.meditracker.model.enums.DoseStatus.TAKEN);
        assertThat(DoseController.status(taken, at, at.plus(Duration.ofHours(5)))).isEqualTo("TAKEN");
        var pending = new org.springbozo.meditracker.model.DoseLog();
        pending.setStatus(org.springbozo.meditracker.model.enums.DoseStatus.PENDING);
        assertThat(DoseController.status(pending, at, at.plus(Duration.ofHours(5)))).isEqualTo("PAST");
    }

    @Test
    void todayListsNowThenUpcomingThenPast() throws Exception {
        // doses every 3 hours today, so whatever time the test runs there's a past, a now and an upcoming one
        jdbc.update("UPDATE prescription SET active = FALSE");
        LocalDate today = LocalDate.now(DUBAI);
        Integer medicineId = jdbc.queryForObject("SELECT medicine_id FROM prescription WHERE prescription_id = ?",
                Integer.class, prescriptionId);
        List<Map<String, String>> schedule = new java.util.ArrayList<>();
        for (int h = 0; h < 24; h += 3) {
            schedule.add(Map.of("dayOfWeek", today.getDayOfWeek().name(), "timeOfDay", String.format("%02d:00", h)));
        }
        mvc.perform(post("/api/prescriptions").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("medicineId", medicineId,
                                "dosage", Map.of("amount", 1, "unit", "CAPSULE"), "startDate", today.toString(),
                                "timeZone", "Asia/Dubai", "schedule", schedule))))
                .andExpect(status().isCreated());

        JsonNode list = json.readTree(mvc.perform(get("/api/doses/today").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString());
        // doses from before the prescription was added are shown too
        assertThat(list).hasSize(8);
        List<String> statuses = new java.util.ArrayList<>();
        list.forEach(d -> statuses.add(d.get("status").asText()));
        int lastNow = statuses.lastIndexOf("NOW"), firstUpcoming = statuses.indexOf("UPCOMING"), firstPast = statuses.indexOf("PAST");
        if (lastNow >= 0 && firstUpcoming >= 0) assertThat(lastNow).isLessThan(firstUpcoming);
        if (firstUpcoming >= 0 && firstPast >= 0) assertThat(statuses.lastIndexOf("UPCOMING")).isLessThan(firstPast);
        if (lastNow >= 0 && firstPast >= 0) assertThat(lastNow).isLessThan(firstPast);
        assertThat(statuses).doesNotContain("DUE", "MISSED");
    }

    @Test
    void notificationButtonsNeedTheRightToken() throws Exception {
        reminders.tick(eightAm.plusSeconds(30));
        JsonNode reminder = sentPayloads().get(0);
        int doseId = reminder.get("doseId").asInt();

        for (Map<String, Object> bad : List.<Map<String, Object>>of(
                Map.of("doseId", doseId, "token", "forged", "status", "TAKEN"),
                Map.of("doseId", doseId + 1, "token", reminder.get("token").asText(), "status", "TAKEN"),
                Map.of("doseId", doseId, "token", reminder.get("token").asText(), "status", "PENDING"),
                Map.of("doseId", "x", "token", "y", "status", "TAKEN"))) {
            mvc.perform(post("/api/doses/quick").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(bad)))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void loggingEarlyMeansNoReminder() throws Exception {
        mvc.perform(post("/api/doses").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("prescriptionId", String.valueOf(prescriptionId),
                                "scheduledAt", eightAm.toString(), "status", "TAKEN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TAKEN"));
        reminders.tick(eightAm.plusSeconds(30));
        assertThat(sentPayloads()).isEmpty();

        // undo puts it back to not logged
        mvc.perform(post("/api/doses").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("prescriptionId", String.valueOf(prescriptionId),
                                "scheduledAt", eightAm.toString(), "status", "PENDING"))))
                .andExpect(status().isOk());
    }

    @Test
    void cannotLogTimesThatArentOnTheSchedule() throws Exception {
        for (Map<String, String> bad : List.of(
                Map.of("prescriptionId", String.valueOf(prescriptionId), "scheduledAt", eightAm.plusSeconds(60).toString(), "status", "TAKEN"),
                Map.of("prescriptionId", String.valueOf(prescriptionId), "scheduledAt", eightAm.minus(Duration.ofDays(10)).toString(), "status", "TAKEN"),
                Map.of("prescriptionId", String.valueOf(prescriptionId), "scheduledAt", eightAm.toString(), "status", "EATEN"))) {
            mvc.perform(post("/api/doses").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(bad)))
                    .andExpect(status().isBadRequest());
        }
        // someone else's prescription
        mvc.perform(post("/api/doses").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("prescriptionId", "999999", "scheduledAt", eightAm.toString(), "status", "TAKEN"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void lateServerSkipsStaleRemindersAndDeadDevicesAreDropped() throws Exception {
        // server was down at 08:00 and comes back at 08:20: too late to remind
        reminders.tick(eightAm.plus(Duration.ofMinutes(20)));
        assertThat(sentPayloads()).isEmpty();

        // the push service says this device is gone: its subscription is removed
        when(push.send(anyString(), anyString(), anyString(), anyString())).thenReturn(WebPushService.Result.GONE);
        reminders.tick(eightAm.plus(Duration.ofDays(1)).plusSeconds(30));
        clearInvocations(push);
        reminders.tick(eightAm.plus(Duration.ofDays(2)).plusSeconds(30));
        verify(push, never()).send(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void subscriptionsMustPointAtARealPushService() throws Exception {
        for (String endpoint : List.of("https://evil.com/push", "http://fcm.googleapis.com/x", "https://localhost/x")) {
            mvc.perform(post("/api/push/subscriptions").header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("endpoint", endpoint, "keys", Map.of("p256dh", "a", "auth", "b")))))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/push/subscriptions").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void scheduleMathHandlesWeekdaysEndDatesAndDst() {
        Prescription p = new Prescription();
        p.setActive(true);
        p.setTimeZone("America/New_York");
        p.setStartDate(LocalDate.of(2026, 3, 1));
        p.setEndDate(LocalDate.of(2026, 3, 31));
        p.setDosage(new Dosage(DosageUnit.TABLET, BigDecimal.valueOf(2)));
        p.setSchedule(Set.of(new ScheduleEntry(DayOfWeek.SUNDAY, LocalTime.of(2, 30)),
                new ScheduleEntry(DayOfWeek.SUNDAY, LocalTime.of(21, 0))));

        // 8 March 2026: clocks jump 02:00 -> 03:00 in New York, so 02:30 happens at 03:30 EDT
        List<Instant> dstDay = ReminderService.occurrencesOn(p, LocalDate.of(2026, 3, 8));
        assertThat(dstDay).containsExactly(Instant.parse("2026-03-08T07:30:00Z"), Instant.parse("2026-03-09T01:00:00Z"));
        assertThat(ReminderService.occurrencesOn(p, LocalDate.of(2026, 3, 9))).as("Monday").isEmpty();
        assertThat(ReminderService.occurrencesOn(p, LocalDate.of(2026, 4, 5))).as("after end date").isEmpty();
        assertThat(ReminderService.dosageLabel(p)).isEqualTo("2 tablets");

        p.setTimeZone("Not/AZone");
        assertThat(ReminderService.occurrencesOn(p, LocalDate.of(2026, 3, 8))).as("bad zone falls back to UTC")
                .contains(Instant.parse("2026-03-08T21:00:00Z"));
    }
}
