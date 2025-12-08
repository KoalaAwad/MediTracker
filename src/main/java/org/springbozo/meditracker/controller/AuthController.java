package org.springbozo.meditracker.controller;

import org.springbozo.meditracker.DAO.RegistrationDto;
import org.springbozo.meditracker.model.User;
import org.springbozo.meditracker.security.JwtUtil;
import org.springbozo.meditracker.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;
    private final UserDetailsService userDetailsService;
    private final UserService userService;
    private final JwtUtil jwtUtil;


    public AuthController(PasswordEncoder passwordEncoder, AuthenticationManager authManager, UserDetailsService userDetailsService, UserService userService, JwtUtil jwtUtil) {
        this.passwordEncoder = passwordEncoder;
        this.authManager = authManager;
        this.userDetailsService = userDetailsService;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String rawEmail = body.get("email");
        String rawPassword = body.get("password");

        if (rawEmail == null || rawEmail.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email and password are required"));
        }

        String email = rawEmail.trim().toLowerCase();

        try {
            Authentication authentication = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, rawPassword)
            );

            String username = authentication.getName();
            if (username == null || username.isBlank()) {
                username = email;
            }

            // Fetch user to get roles
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Extract role names
            String roles = user.getRoles().stream()
                    .map(role -> role.getRoleName())
                    .reduce((a, b) -> a + "," + b)
                    .orElse("USER");

            return ResponseEntity.ok(Map.of(
                    "token", jwtUtil.generateToken(username),
                    "email", email,
                    "role", roles
            ));
        } catch (Exception ex) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegistrationDto dto) {
        boolean created = userService.register(dto);
        if (!created) {
            return ResponseEntity.badRequest().body(Map.of("error", "Registration failed or user already exists"));
        }
        return ResponseEntity.ok(Map.of("message", "User registered successfully"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
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

            // Fetch user details
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Extract role names
            String roles = user.getRoles().stream()
                    .map(role -> role.getRoleName())
                    .reduce((a, b) -> a + "," + b)
                    .orElse("USER");

            return ResponseEntity.ok(Map.of(
                    "email", user.getEmail(),
                    "name", user.getName(),
                    "role", roles,
                    "userId", user.getId()
            ));
        } catch (Exception ex) {
            return ResponseEntity.status(401).body(Map.of("error", "Authentication failed"));
        }
    }
}