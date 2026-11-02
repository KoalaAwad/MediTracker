package org.springbozo.meditracker.controller;

import org.springbozo.meditracker.DAO.RegistrationDto;
import org.springbozo.meditracker.model.User;
import org.springbozo.meditracker.model.Patient;
import org.springbozo.meditracker.model.Doctor;
import org.springbozo.meditracker.repository.PatientRepository;
import org.springbozo.meditracker.repository.DoctorRepository;
import org.springbozo.meditracker.config.JwtUtil;
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

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;
    private final UserDetailsService userDetailsService;
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AuthTokenService authTokenService;
    private final MailService mailService;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");


    public AuthController(PasswordEncoder passwordEncoder, AuthenticationManager authManager, UserDetailsService userDetailsService, UserService userService, JwtUtil jwtUtil, PatientRepository patientRepository, DoctorRepository doctorRepository, AuthTokenService authTokenService, MailService mailService) {
        this.authTokenService = authTokenService;
        this.mailService = mailService;
        this.passwordEncoder = passwordEncoder;
        this.authManager = authManager;
        this.userDetailsService = userDetailsService;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
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
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
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

        // Duplicate email check with clear message
        if (userService.emailExists(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "This email is already registered"));
        }

        User created = userService.register(dto);
        if (created == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Registration failed"));
        }
        mailService.sendVerification(created.getEmail(), created.getName(),
                authTokenService.create(created, AuthTokenType.VERIFY_EMAIL));
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Check your email to verify your account"));
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

    @PutMapping("/me")
    public ResponseEntity<?> updateCurrentUser(@RequestHeader("Authorization") String authHeader, @RequestBody Map<String, Object> body) {
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

            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Update basic fields
            if (body.containsKey("name")) {
                Object nameObj = body.get("name");
                if (nameObj != null) user.setName(nameObj.toString());
            }

            // 'user' remains the same reference from earlier fetch; don't reassign so lambdas can capture it

            // Update patient info if user has patient role
            boolean isPatient = user.getRoles().stream().anyMatch(r -> r.getRoleName().equals(org.springbozo.meditracker.constants.Constants.PATIENT_ROLE));
            if (isPatient && body.containsKey("x")) {
                Object patientObj = body.get("patient");
                if (patientObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> pmap = (Map<String, Object>) patientObj;
                    Patient patient = patientRepository.findByUserId(user.getId()).orElseGet(() -> {
                        Patient p = new Patient();
                        p.setUser(user);
                        p.setActive(true);
                        return p;
                    });
                    if (pmap.containsKey("phone")) patient.setPhone((String) pmap.get("phone"));
                    if (pmap.containsKey("address")) patient.setAddress((String) pmap.get("address"));
                    if (pmap.containsKey("bloodType")) patient.setBloodType((String) pmap.get("bloodType"));
                    patientRepository.save(patient);
                }
            }

            // Update doctor info if user has doctor role
            boolean isDoctor = user.getRoles().stream().anyMatch(r -> r.getRoleName().equals(org.springbozo.meditracker.constants.Constants.DOCTOR_ROLE));
            if (isDoctor && body.containsKey("doctor")) {
                Object doctorObj = body.get("doctor");
                if (doctorObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> dmap = (Map<String, Object>) doctorObj;
                    Optional<Doctor> docOpt = doctorRepository.findByUserId(user.getId());
                    Doctor doctor = docOpt.orElseGet(() -> {
                        Doctor d = new Doctor();
                        d.setUser(user);
                        d.setActive(true);
                        return d;
                    });
                    if (dmap.containsKey("firstName")) doctor.setFirstName((String) dmap.get("firstName"));
                    if (dmap.containsKey("lastName")) doctor.setLastName((String) dmap.get("lastName"));
                    if (dmap.containsKey("specialization")) doctor.setSpecialization((String) dmap.get("specialization"));
                    if (dmap.containsKey("phone")) doctor.setPhone((String) dmap.get("phone"));
                    doctorRepository.save(doctor);
                }
            }

            // persist user changes through service layer
            userService.save(user);

            return ResponseEntity.ok(Map.of("message", "Profile updated"));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(400).body(Map.of("error", "Failed to update profile"));
        }
    }
}