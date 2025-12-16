package org.springbozo.meditracker.controller;

import org.springbozo.meditracker.model.Prescription;
import org.springbozo.meditracker.model.embedded.ScheduleEntry;
import org.springbozo.meditracker.security.JwtUtil;
import org.springbozo.meditracker.service.PrescriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/prescriptions")
@CrossOrigin
public class PrescriptionRestController {

    private final PrescriptionService prescriptionService;
    private final JwtUtil jwtUtil;

    @Autowired
    public PrescriptionRestController(PrescriptionService prescriptionService, JwtUtil jwtUtil) {
        this.prescriptionService = prescriptionService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * POST /api/prescriptions - Create prescription for authenticated patient
     */
    @PostMapping
    public ResponseEntity<?> createPrescription(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody PrescriptionService.PrescriptionCreateDto dto) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body(Map.of("error", "Missing or invalid Authorization header"));
            }

            String token = authHeader.substring(7);
            if (!jwtUtil.ValidateJwtToken(token)) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid or expired token"));
            }

            String email = jwtUtil.getUserFromToken(token);

            Prescription prescription = prescriptionService.createPrescriptionForUser(email, dto);

            return ResponseEntity.status(201).body(Map.of(
                    "message", "Prescription created successfully",
                    "prescriptionId", prescription.getId()
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to create prescription: " + e.getMessage()));
        }
    }

    /**
     * GET /api/prescriptions/me - Get all prescriptions for authenticated patient
     */
    @GetMapping("/me")
    public ResponseEntity<?> getMyPrescriptions(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body(Map.of("error", "Missing or invalid Authorization header"));
            }

            String token = authHeader.substring(7);
            if (!jwtUtil.ValidateJwtToken(token)) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid or expired token"));
            }

            String email = jwtUtil.getUserFromToken(token);

            List<Prescription> prescriptions = prescriptionService.getPrescriptionsForUser(email);

            // Convert to DTOs
            List<PrescriptionService.PrescriptionResponseDto> responseDtos = prescriptions.stream()
                    .map(this::toPrescriptionResponseDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(responseDtos);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch prescriptions: " + e.getMessage()));
        }
    }

    /**
     * Helper method to convert Prescription entity to DTO
     */
    private PrescriptionService.PrescriptionResponseDto toPrescriptionResponseDto(Prescription prescription) {
        PrescriptionService.PrescriptionResponseDto dto = new PrescriptionService.PrescriptionResponseDto();

        dto.setId(prescription.getId());
        dto.setMedicineId(prescription.getMedicine().getId());
        dto.setMedicineName(prescription.getMedicine().getName());
        dto.setDosageAmount(prescription.getDosage().getAmount());
        dto.setDosageUnit(prescription.getDosage().getUnit().toString());
        dto.setStartDate(prescription.getStartDate().toString());
        dto.setEndDate(prescription.getEndDate() != null ? prescription.getEndDate().toString() : null);
        dto.setTimeZone(prescription.getTimeZone());

        // Convert schedule
        List<PrescriptionService.ScheduleEntryDto> scheduleDto = new ArrayList<>();
        for (ScheduleEntry entry : prescription.getSchedule()) {
            PrescriptionService.ScheduleEntryDto entryDto = new PrescriptionService.ScheduleEntryDto();
            entryDto.setDayOfWeek(entry.getDayOfWeek().toString());
            entryDto.setTimeOfDay(entry.getTimeOfDay().toString());
            scheduleDto.add(entryDto);
        }
        dto.setSchedule(scheduleDto);

        return dto;
    }
}

