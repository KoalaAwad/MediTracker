package org.springbozo.meditracker.controller;

import lombok.extern.slf4j.Slf4j;
import org.springbozo.meditracker.DAO.ProfileDto;
import org.springbozo.meditracker.DAO.PatientProfileDto;
import org.springbozo.meditracker.model.User;
import org.springbozo.meditracker.model.Patient;
import org.springbozo.meditracker.config.JwtUtil;
import org.springbozo.meditracker.service.UserService;
import org.springbozo.meditracker.repository.PatientRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class ProfileRestController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final PatientRepository patientRepository;

    public ProfileRestController(UserService userService, JwtUtil jwtUtil, PatientRepository patientRepository) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.patientRepository = patientRepository;
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body(Map.of("error", "Missing or invalid Authorization header"));
            }

            String token = authHeader.substring(7);

            if (!jwtUtil.ValidateJwtToken(token)) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid or expired token"));
            }

            String email = jwtUtil.getUserFromToken(token);
            if (email == null || email.isBlank()) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid token payload"));
            }

            Optional<User> userOpt = userService.findByEmail(email);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }

            User user = userOpt.get();
            ProfileDto profile = userService.buildProfileDto(user);

            return ResponseEntity.ok(profile);

        } catch (Exception ex) {
            log.error("Internal server error", ex);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }

    // ...existing getProfile method...

    @PutMapping("/profile/patient")
    public ResponseEntity<?> updatePatientProfile(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody PatientProfileDto dto) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body(Map.of("error", "Missing or invalid Authorization header"));
            }

            String token = authHeader.substring(7);

            if (!jwtUtil.ValidateJwtToken(token)) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid or expired token"));
            }

            String email = jwtUtil.getUserFromToken(token);
            if (email == null || email.isBlank()) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid token payload"));
            }

            Optional<User> userOpt = userService.findByEmail(email);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }

            User user = userOpt.get();
            Optional<Patient> patientOpt = patientRepository.findByUserId(user.getId());
            if (patientOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "Patient profile not found"));
            }

            Patient patient = patientOpt.get();
            if (dto.getName() != null) patient.setName(dto.getName());
            if (dto.getGender() != null) patient.setGender(dto.getGender());
            if (dto.getDateOfBirth() != null) patient.setDateOfBirth(dto.getDateOfBirth());
            if (dto.getPhone() != null) patient.setPhone(dto.getPhone());
            if (dto.getAddress() != null) patient.setAddress(dto.getAddress());
            if (dto.getBloodType() != null) patient.setBloodType(dto.getBloodType());
            if (dto.getAllergies() != null) patient.setAllergies(dto.getAllergies());
            if (dto.getMedicalHistory() != null) patient.setMedicalHistory(dto.getMedicalHistory());

            patientRepository.save(patient);
            return ResponseEntity.ok(Map.of("message", "Patient profile updated successfully"));

        } catch (Exception ex) {
            log.error("Internal server error", ex);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }
}

