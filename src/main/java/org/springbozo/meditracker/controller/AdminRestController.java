package org.springbozo.meditracker.controller;

import lombok.extern.slf4j.Slf4j;
import org.springbozo.meditracker.model.User;
import org.springbozo.meditracker.config.JwtUtil;
import org.springbozo.meditracker.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@CrossOrigin
public class AdminRestController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AdminRestController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(
            @RequestHeader("Authorization") String authHeader) {
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

            List<User> users = userService.getAllUsers();

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
                return userMap;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(Map.of("users", userDtos));

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

            // the admin account is removed through Profile -> Delete account, which has its own checks
            if (userService.findByEmail(email).map(u -> u.getId() == userId).orElse(false)) {
                return ResponseEntity.badRequest().body(Map.of("error", "You can't delete your own account here"));
            }

            boolean deleted = userService.deleteUser(userId);
            if (!deleted) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found or could not be deleted"));
            }

            return ResponseEntity.ok(Map.of("message", "User deleted successfully"));

        } catch (Exception ex) {
            log.error("Internal server error", ex);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }
}
