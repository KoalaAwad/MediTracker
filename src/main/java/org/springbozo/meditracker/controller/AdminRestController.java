package org.springbozo.meditracker.controller;

import org.springbozo.meditracker.model.Role;
import org.springbozo.meditracker.model.User;
import org.springbozo.meditracker.config.JwtUtil;
import org.springbozo.meditracker.service.UserService;
import org.springbozo.meditracker.repository.PatientRepository;
import org.springbozo.meditracker.repository.DoctorRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin
public class AdminRestController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    public AdminRestController(UserService userService, JwtUtil jwtUtil, PatientRepository patientRepository, DoctorRepository doctorRepository) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(name = "role", required = false) String role,
            @RequestParam(name = "only", required = false, defaultValue = "false") boolean only) {
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

            if (!userService.hasAdminRole(email)) {
                return ResponseEntity.status(403).body(Map.of("error", "Forbidden: Admin access required"));
            }

            List<User> users;
            if (role != null && !role.isBlank()) {
                users = userService.getUsersFilteredByRole(role, only);
            } else {
                users = userService.getAllUsers();
            }

            List<Map<String, Object>> userDtos = users.stream().map(user -> {
                String roles = user.getRoles().stream()
                        .map(r -> r.getRoleName())
                        .reduce((a, b) -> a + "," + b)
                        .orElse("USER");

                Map<String, Object> userMap = new HashMap<>();
                userMap.put("userId", user.getId());
                userMap.put("name", user.getName());
                userMap.put("email", user.getEmail());
                userMap.put("role", roles);
                userMap.put("createdAt", user.getCreatedAt().toString());
                // include patient/doctor active flags
                boolean patientActive = patientRepository.findByUserId(user.getId()).map(p -> p.isActive()).orElse(false);
                boolean doctorActive = doctorRepository.findByUserId(user.getId()).map(d -> d.isActive()).orElse(false);
                userMap.put("patientActive", patientActive);
                userMap.put("doctorActive", doctorActive);
                return userMap;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(Map.of("users", userDtos));

        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }

    @GetMapping("/roles")
    public ResponseEntity<?> getAvailableRoles(@RequestHeader("Authorization") String authHeader) {
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

            if (!userService.hasAdminRole(email)) {
                return ResponseEntity.status(403).body(Map.of("error", "Forbidden: Admin access required"));
            }

            List<Role> roles = userService.getAllRoles();
            List<String> roleNames = roles.stream()
                    .map(Role::getRoleName)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of("roles", roleNames));

        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }

    @PutMapping("/users/{userId}/roles")
    public ResponseEntity<?> updateUserRoles(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable int userId,
            @RequestBody Map<String, List<String>> body) {
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

            if (!userService.hasAdminRole(email)) {
                return ResponseEntity.status(403).body(Map.of("error", "Forbidden: Admin access required"));
            }

            List<String> roles = body.get("roles");
            if (roles == null || roles.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Roles list cannot be empty"));
            }

            boolean updated = userService.updateUserRoles(userId, roles);
            if (!updated) {
                return ResponseEntity.badRequest().body(Map.of("error", "Failed to update user roles"));
            }

            return ResponseEntity.ok(Map.of("message", "User roles updated successfully"));

        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<?> deleteUser(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable int userId) {
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

            if (!userService.hasAdminRole(email)) {
                return ResponseEntity.status(403).body(Map.of("error", "Forbidden: Admin access required"));
            }

            boolean deleted = userService.deleteUser(userId);
            if (!deleted) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found or could not be deleted"));
            }

            return ResponseEntity.ok(Map.of("message", "User deleted successfully"));

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error: " + ex.getMessage()));
        }
    }
}
