package org.springbozo.meditracker.controller;

import org.springbozo.meditracker.DAO.RegistrationDto;
import org.springbozo.meditracker.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springbozo.meditracker.config.JwtUtil;
import org.springbozo.meditracker.config.RateLimiter;
import org.springbozo.meditracker.model.enums.AuthTokenType;
import org.springbozo.meditracker.service.AuthTokenService;
import org.springbozo.meditracker.service.MailService;
import org.springbozo.meditracker.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;
    private final UserDetailsService userDetailsService;
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final AuthTokenService authTokenService;
    private final MailService mailService;

    private final RateLimiter rateLimiter;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final String REGISTERED_MESSAGE = "Check your email to verify your account";
    private static final int MAX_LOGIN_FAILURES = 10;
    private static final Duration LOGIN_FAILURE_WINDOW = Duration.ofMinutes(15);


    public AuthController(PasswordEncoder passwordEncoder, AuthenticationManager authManager, UserDetailsService userDetailsService, UserService userService, JwtUtil jwtUtil, AuthTokenService authTokenService, MailService mailService, RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
        this.authTokenService = authTokenService;
        this.mailService = mailService;
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

        // per-account limit on failed logins, on top of the per-IP limit in RateLimitFilter
        String failuresKey = "login-failures|" + email;
        if (rateLimiter.isOverLimit(failuresKey, MAX_LOGIN_FAILURES, LOGIN_FAILURE_WINDOW)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Too many failed attempts. Try again in 15 minutes or reset your password."));
        }

        Authentication authentication;
        try {
            authentication = authManager.authenticate(new UsernamePasswordAuthenticationToken(email, rawPassword));
        } catch (Exception ex) {
            rateLimiter.hit(failuresKey, LOGIN_FAILURE_WINDOW);
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
        }
        rateLimiter.clear(failuresKey);

        try {

            String username = authentication.getName();
            if (username == null || username.isBlank()) {
                username = email;
            }

            // Fetch user to get roles
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // checked after the password so this doesn't reveal which emails have accounts
            if (!user.isEmailVerified()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "error", "Please verify your email before logging in",
                        "code", "EMAIL_NOT_VERIFIED"
                ));
            }

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
            log.error("Login failed after authentication for {}", email, ex);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegistrationDto dto) {
        // Basic validation
        String rawEmail = dto != null ? dto.getEmail() : null;
        String email = (rawEmail != null) ? rawEmail.trim().toLowerCase() : null;
        String password = (dto != null) ? dto.getPassword() : null;

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email and password are required"));
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Enter a valid email address"));
        }
        String passwordError = UserService.validatePassword(password);
        if (passwordError != null) {
            return ResponseEntity.badRequest().body(Map.of("error", passwordError));
        }

        // A taken email gets the same answer as a new one, so register can't be used to find out
        // which emails have accounts. If that account was never verified, it gets a fresh link.
        Optional<User> existing = userService.findByEmail(email);
        if (existing.isPresent()) {
            existing.filter(u -> !u.isEmailVerified())
                    .filter(u -> !authTokenService.recentlySent(u, AuthTokenType.VERIFY_EMAIL))
                    .ifPresent(u -> mailService.sendVerification(u.getEmail(), u.getName(),
                            authTokenService.create(u, AuthTokenType.VERIFY_EMAIL)));
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", REGISTERED_MESSAGE));
        }

        User created = userService.register(dto);
        if (created == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Registration failed"));
        }
        mailService.sendVerification(created.getEmail(), created.getName(),
                authTokenService.create(created, AuthTokenType.VERIFY_EMAIL));
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", REGISTERED_MESSAGE));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestBody Map<String, String> body) {
        Optional<User> userOpt = authTokenService.consume(body.get("token"), AuthTokenType.VERIFY_EMAIL);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "This link is invalid or has expired"));
        }
        User user = userOpt.get();
        user.setEmailVerified(true);
        userService.save(user);
        return ResponseEntity.ok(Map.of("message", "Email verified. You can now log in."));
    }

    // Always answers the same way so it can't be used to find out which emails have accounts
    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestBody Map<String, String> body) {
        userService.findByEmail(body.get("email"))
                .filter(u -> !u.isEmailVerified())
                .filter(u -> !authTokenService.recentlySent(u, AuthTokenType.VERIFY_EMAIL))
                .ifPresent(u -> mailService.sendVerification(u.getEmail(), u.getName(),
                        authTokenService.create(u, AuthTokenType.VERIFY_EMAIL)));
        return ResponseEntity.ok(Map.of("message", "If that account needs verifying, we've sent a new link"));
    }

    // Always answers the same way so it can't be used to find out which emails have accounts
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        userService.findByEmail(body.get("email"))
                .filter(u -> !authTokenService.recentlySent(u, AuthTokenType.PASSWORD_RESET))
                .ifPresent(u -> mailService.sendPasswordReset(u.getEmail(), u.getName(),
                        authTokenService.create(u, AuthTokenType.PASSWORD_RESET)));
        return ResponseEntity.ok(Map.of("message", "If an account exists for that email, we've sent a reset link"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        String password = body.get("password");
        String passwordError = UserService.validatePassword(password);
        if (passwordError != null) {
            return ResponseEntity.badRequest().body(Map.of("error", passwordError));
        }
        Optional<User> userOpt = authTokenService.consume(body.get("token"), AuthTokenType.PASSWORD_RESET);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "This link is invalid or has expired"));
        }
        User user = userOpt.get();
        // clicking a link sent to the inbox also proves the email is theirs
        user.setEmailVerified(true);
        userService.changePassword(user, password);
        return ResponseEntity.ok(Map.of("message", "Password updated. You can now log in."));
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

    // Deletes the logged-in user's account and everything linked to it (patient, prescriptions, tokens)
    @DeleteMapping("/me")
    public ResponseEntity<?> deleteCurrentUser(@RequestHeader("Authorization") String authHeader,
                                               @RequestBody Map<String, String> body) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Missing or invalid Authorization header"));
        }
        String token = authHeader.substring(7);
        if (!jwtUtil.ValidateJwtToken(token)) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid or expired token"));
        }
        Optional<User> userOpt = userService.findByEmail(jwtUtil.getUserFromToken(token));
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token payload"));
        }
        User user = userOpt.get();

        String password = body.get("password");
        if (password == null || !passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.status(403).body(Map.of("error", "Incorrect password"));
        }
        if (userService.hasAdminRole(user.getEmail())
                && userService.getUsersByRole(org.springbozo.meditracker.constants.Constants.ADMIN_ROLE).size() <= 1) {
            return ResponseEntity.status(409).body(Map.of("error", "You are the only admin. Make someone else an admin first."));
        }
        if (!userService.deleteUser(user.getId())) {
            return ResponseEntity.status(500).body(Map.of("error", "Could not delete account"));
        }
        return ResponseEntity.ok(Map.of("message", "Account deleted"));
    }
}