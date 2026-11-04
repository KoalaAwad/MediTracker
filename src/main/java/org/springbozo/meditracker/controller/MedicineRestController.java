package org.springbozo.meditracker.controller;

import org.springbozo.meditracker.model.Medicine;
import org.springbozo.meditracker.service.DailyMedService;
import org.springbozo.meditracker.service.MedicineService;
import org.springbozo.meditracker.service.MedicineSyncService;
import org.springbozo.meditracker.service.UserService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

// Spring Security already requires a valid token for everything under /api/medicine
@RestController
@RequestMapping("/api/medicine")
public class MedicineRestController {

    private static final int MAX_PAGE_SIZE = 100;

    private final MedicineService medicineService;
    private final MedicineSyncService syncService;
    private final DailyMedService dailyMed;
    private final UserService userService;

    public MedicineRestController(MedicineService medicineService, MedicineSyncService syncService,
                                  DailyMedService dailyMed, UserService userService) {
        this.medicineService = medicineService;
        this.syncService = syncService;
        this.dailyMed = dailyMed;
        this.userService = userService;
    }

    @GetMapping("/paged")
    public ResponseEntity<?> getPagedMedicines(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size,
                                               @RequestParam(required = false) String q) {
        var result = medicineService.listMedicinesPaged(page, Math.min(size, MAX_PAGE_SIZE), q);
        return ResponseEntity.ok(Map.of(
                "content", result.getContent(),
                "page", result.getNumber(),
                "size", result.getSize(),
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getMedicineById(@PathVariable int id) {
        return medicineService.getMedicineByIdIncludingInactive(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(404).body(Map.of("error", "Medicine not found")));
    }

    /** Package label images and the pill description from the official DailyMed label. */
    @GetMapping("/{id}/label")
    public ResponseEntity<?> getLabel(@PathVariable int id) {
        Optional<Medicine> med = medicineService.getMedicineByIdIncludingInactive(id);
        if (med.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Medicine not found"));
        }
        String setId = med.get().getSplSetId();
        Map<String, Object> body = new HashMap<>();
        body.put("available", false);
        dailyMed.label(setId).ifPresent(label -> {
            body.put("available", true);
            body.put("images", label.imagesFor(med.get().getProductNdc(), med.get().getStrength()).stream()
                    .map(name -> "/api/medicine-images/" + setId + "/" + name).toList());
            body.put("appearance", label.appearanceByNdc().get(med.get().getProductNdc()));
            body.put("dailyMedUrl", "https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=" + setId);
        });
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofHours(6)).cachePrivate()).body(body);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMedicine(@PathVariable int id, Authentication auth) {
        if (!userService.hasAdminRole(auth.getName())) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden: Admin access required"));
        }
        if (!medicineService.deleteMedicine(id)) {
            return ResponseEntity.status(404).body(Map.of("error", "Medicine not found"));
        }
        // Soft delete - medicine is marked as inactive but prescriptions remain intact
        return ResponseEntity.ok(Map.of("message", "Medicine marked as inactive. Existing prescriptions are preserved."));
    }

    /** Re-imports the openFDA NDC directory in the background. */
    @PostMapping("/sync")
    public ResponseEntity<?> startSync(Authentication auth) {
        if (!userService.hasAdminRole(auth.getName())) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden: Admin access required"));
        }
        if (syncService.isRunning()) {
            return ResponseEntity.status(409).body(Map.of("error", "A sync is already running"));
        }
        syncService.syncAsync();
        return ResponseEntity.accepted().body(Map.of("message", "Sync started"));
    }

    @GetMapping("/sync/status")
    public ResponseEntity<?> syncStatus(Authentication auth) {
        if (!userService.hasAdminRole(auth.getName())) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden: Admin access required"));
        }
        var s = syncService.getStatus();
        Map<String, Object> body = new HashMap<>();
        body.put("running", s.running());
        body.put("startedAt", s.startedAt());
        body.put("finishedAt", s.finishedAt());
        body.put("imported", s.imported());
        body.put("error", s.error());
        body.put("total", medicineService.countActive());
        return ResponseEntity.ok(body);
    }
}
