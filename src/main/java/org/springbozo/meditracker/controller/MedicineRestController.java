package org.springbozo.meditracker.controller;

import org.springbozo.meditracker.model.Medicine;
import org.springbozo.meditracker.config.JwtUtil;
import org.springbozo.meditracker.service.MedicineService;
import org.springbozo.meditracker.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/medicine")
@CrossOrigin
public class MedicineRestController {

    private final MedicineService medicineService;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    public MedicineRestController(MedicineService medicineService, UserService userService, JwtUtil jwtUtil) {
        this.medicineService = medicineService;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping
    public ResponseEntity<?> getAllMedicines(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body(Map.of("error", "Missing or invalid Authorization header"));
            }

            String token = authHeader.substring(7);
            if (!jwtUtil.ValidateJwtToken(token)) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid or expired token"));
            }

            List<Medicine> medicines = medicineService.listMedicines();
            return ResponseEntity.ok(medicines);

        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getMedicineById(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable int id) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body(Map.of("error", "Missing or invalid Authorization header"));
            }

            String token = authHeader.substring(7);
            if (!jwtUtil.ValidateJwtToken(token)) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid or expired token"));
            }

            Optional<Medicine> medicine = medicineService.getMedicineById(id);
            if (medicine.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "Medicine not found"));
            }

            return ResponseEntity.ok(medicine.get());

        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }

    @PostMapping
    public ResponseEntity<?> createMedicine(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Medicine medicine) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body(Map.of("error", "Missing or invalid Authorization header"));
            }

            String token = authHeader.substring(7);
            if (!jwtUtil.ValidateJwtToken(token)) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid or expired token"));
            }

            String email = jwtUtil.getUserFromToken(token);
            if (!userService.hasAdminRole(email) && !userService.hasDoctorRole(email)) {
                return ResponseEntity.status(403).body(Map.of("error", "Forbidden: Admin or Doctor access required"));
            }

            medicine.setId(0);
            Medicine saved = medicineService.save(medicine);
            return ResponseEntity.status(201).body(saved);

        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateMedicine(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable int id,
            @RequestBody Medicine medicine) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body(Map.of("error", "Missing or invalid Authorization header"));
            }

            String token = authHeader.substring(7);
            if (!jwtUtil.ValidateJwtToken(token)) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid or expired token"));
            }

            String email = jwtUtil.getUserFromToken(token);
            if (!userService.hasAdminRole(email)) {
                return ResponseEntity.status(403).body(Map.of("error", "Forbidden: Admin access required"));
            }

            if (!medicineService.getMedicineById(id).isPresent()) {
                return ResponseEntity.status(404).body(Map.of("error", "Medicine not found"));
            }

            medicine.setId(id);
            Medicine updated = medicineService.save(medicine);
            return ResponseEntity.ok(updated);

        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMedicine(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable int id) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body(Map.of("error", "Missing or invalid Authorization header"));
            }

            String token = authHeader.substring(7);
            if (!jwtUtil.ValidateJwtToken(token)) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid or expired token"));
            }

            String email = jwtUtil.getUserFromToken(token);
            if (!userService.hasAdminRole(email)) {
                return ResponseEntity.status(403).body(Map.of("error", "Forbidden: Admin access required"));
            }

            boolean deleted = medicineService.deleteMedicine(id);
            if (!deleted) {
                return ResponseEntity.status(404).body(Map.of("error", "Medicine not found"));
            }

            return ResponseEntity.ok(Map.of("message", "Medicine deleted successfully"));

        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }
}

