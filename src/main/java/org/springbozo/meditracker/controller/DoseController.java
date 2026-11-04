package org.springbozo.meditracker.controller;

import org.springbozo.meditracker.model.DoseLog;
import org.springbozo.meditracker.model.Patient;
import org.springbozo.meditracker.model.Prescription;
import org.springbozo.meditracker.model.User;
import org.springbozo.meditracker.model.enums.DoseStatus;
import org.springbozo.meditracker.repository.DoseLogRepository;
import org.springbozo.meditracker.repository.PatientRepository;
import org.springbozo.meditracker.repository.PrescriptionRepository;
import org.springbozo.meditracker.service.ReminderService;
import org.springbozo.meditracker.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/doses")
public class DoseController {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private final UserService userService;
    private final PatientRepository patients;
    private final PrescriptionRepository prescriptions;
    private final DoseLogRepository doses;
    private final ReminderService reminders;

    public DoseController(UserService userService, PatientRepository patients, PrescriptionRepository prescriptions,
                          DoseLogRepository doses, ReminderService reminders) {
        this.userService = userService;
        this.patients = patients;
        this.prescriptions = prescriptions;
        this.doses = doses;
        this.reminders = reminders;
    }

    /**
     * Today's doses for every active prescription, in time order, each with its status:
     * TAKEN, SKIPPED, UPCOMING, DUE (time has come, not logged yet) or MISSED (over 2 hours late).
     * "Today" is the prescription's own time zone.
     */
    @GetMapping("/today")
    @Transactional(readOnly = true)
    public ResponseEntity<?> today(Authentication auth) {
        List<Prescription> mine = myActivePrescriptions(auth);
        Instant now = Instant.now();
        Map<String, List<Instant>> byPrescription = new HashMap<>();
        Instant earliest = null, latest = null;
        for (Prescription p : mine) {
            List<Instant> times = ReminderService.occurrencesOn(p, now.atZone(zoneOf(p)).toLocalDate());
            byPrescription.put(String.valueOf(p.getId()), times);
            for (Instant t : times) {
                if (earliest == null || t.isBefore(earliest)) earliest = t;
                if (latest == null || t.isAfter(latest)) latest = t;
            }
        }
        Map<String, DoseLog> logged = earliest == null ? Map.of() : doses.findForPrescriptions(
                        mine.stream().map(Prescription::getId).toList(), earliest, latest.plusSeconds(1)).stream()
                .collect(Collectors.toMap(d -> key(d.getPrescription().getId(), d.getScheduledAt()), Function.identity()));

        List<Map<String, Object>> out = new ArrayList<>();
        for (Prescription p : mine) {
            Instant added = p.getCreatedAt() == null ? null : p.getCreatedAt().truncatedTo(java.time.temporal.ChronoUnit.MINUTES);
            for (Instant at : byPrescription.get(String.valueOf(p.getId()))) {
                DoseLog log = logged.get(key(p.getId(), at));
                // a dose from before the prescription was added isn't "missed", unless someone logged it
                if (log == null && added != null && at.isBefore(added)) continue;
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("prescriptionId", p.getId());
                item.put("medicineId", p.getMedicine().getId());
                item.put("medicineName", ReminderService.medicineLabel(p.getMedicine()));
                item.put("dosage", ReminderService.dosageLabel(p));
                item.put("notes", p.getNotes());
                item.put("scheduledAt", at.toString());
                item.put("localTime", at.atZone(zoneOf(p)).format(HH_MM));
                item.put("status", status(log, at, now));
                item.put("statusAt", log == null || log.getStatusAt() == null ? null : log.getStatusAt().toString());
                out.add(item);
            }
        }
        out.sort(Comparator.comparing((Map<String, Object> m) -> (String) m.get("scheduledAt"))
                .thenComparing(m -> (String) m.get("medicineName")));
        return ResponseEntity.ok(out);
    }

    /** Mark a dose taken or skipped, or back to not logged (PENDING). The dose must be a real scheduled time. */
    @PostMapping
    @Transactional
    public ResponseEntity<?> log(@RequestBody Map<String, String> body, Authentication auth) {
        DoseStatus status = parseStatus(body.get("status"));
        if (status == null) return ResponseEntity.badRequest().body(Map.of("error", "status must be TAKEN, SKIPPED or PENDING"));
        int prescriptionId;
        Instant at;
        try {
            prescriptionId = Integer.parseInt(body.get("prescriptionId"));
            at = Instant.parse(body.get("scheduledAt"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "prescriptionId and scheduledAt are required"));
        }
        Optional<Prescription> p = myActivePrescriptions(auth).stream().filter(x -> x.getId() == prescriptionId).findFirst();
        if (p.isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "Prescription not found"));
        // only doses that exist on the schedule, from yesterday to tomorrow
        LocalDate day = at.atZone(zoneOf(p.get())).toLocalDate();
        LocalDate today = LocalDate.now(zoneOf(p.get()));
        if (day.isBefore(today.minusDays(1)) || day.isAfter(today.plusDays(1))
                || !ReminderService.occurrencesOn(p.get(), day).contains(at)) {
            return ResponseEntity.badRequest().body(Map.of("error", "That time isn't on this prescription's schedule"));
        }
        DoseLog dose = doses.findOne(prescriptionId, at).orElseGet(() -> {
            DoseLog d = new DoseLog();
            d.setPrescription(p.get());
            d.setScheduledAt(at);
            return d;
        });
        apply(dose, status);
        return ResponseEntity.ok(Map.of("status", status(dose, at, Instant.now())));
    }

    /**
     * Used by the Taken / Skip buttons on a notification. The service worker has no login token,
     * so the notification carries a token that's only good for that one dose.
     */
    @PostMapping("/quick")
    @Transactional
    public ResponseEntity<?> quick(@RequestBody Map<String, Object> body) {
        DoseStatus status = parseStatus(String.valueOf(body.get("status")));
        Optional<DoseLog> dose;
        try {
            dose = doses.findById(Integer.parseInt(String.valueOf(body.get("doseId"))));
        } catch (NumberFormatException e) {
            dose = Optional.empty();
        }
        if (status == null || status == DoseStatus.PENDING || dose.isEmpty()
                || !reminders.verifyActionToken(dose.get(), (String) body.get("token"), Instant.now())) {
            return ResponseEntity.status(403).body(Map.of("error", "This link is invalid or has expired"));
        }
        apply(dose.get(), status);
        return ResponseEntity.ok(Map.of("status", status.name()));
    }

    private void apply(DoseLog dose, DoseStatus status) {
        dose.setStatus(status);
        dose.setStatusAt(status == DoseStatus.PENDING ? null : Instant.now());
        doses.save(dose);
    }

    private List<Prescription> myActivePrescriptions(Authentication auth) {
        User user = userService.findByEmail(auth.getName()).orElseThrow();
        Optional<Patient> patient = patients.findByUserId(user.getId());
        return patient.map(pt -> prescriptions.findByPatientIdAndActiveTrue(pt.getId())).orElse(List.of());
    }

    static String status(DoseLog log, Instant at, Instant now) {
        if (log != null && log.getStatus() != DoseStatus.PENDING) return log.getStatus().name();
        if (at.isAfter(now)) return "UPCOMING";
        return Duration.between(at, now).compareTo(Duration.ofHours(2)) > 0 ? "MISSED" : "DUE";
    }

    private static DoseStatus parseStatus(String s) {
        try {
            return DoseStatus.valueOf(s);
        } catch (Exception e) {
            return null;
        }
    }

    private static ZoneId zoneOf(Prescription p) {
        try {
            return ZoneId.of(p.getTimeZone());
        } catch (Exception e) {
            return ZoneOffset.UTC;
        }
    }

    private static String key(int prescriptionId, Instant at) {
        return prescriptionId + "@" + at.getEpochSecond();
    }
}
