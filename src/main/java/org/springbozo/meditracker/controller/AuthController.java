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
                username = email; // fallback
            }

            return ResponseEntity.ok(Map.of("token", jwtUtil.generateToken(username)));
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













//    DEPRECATED - using REST endpoint for login instead
//
//    @RequestMapping(value = "/login", method = {RequestMethod.GET, RequestMethod.POST})
//    public String displayLoginPage(
//            @RequestParam(value = "error", required = false) String error,
//            @RequestParam(value = "logout", required = false) String logout,
//            @RequestParam(value = "register", required = false) String register,
//            Model model) {
//
//        String errorMessage = null;
//        if (error != null) {
//            errorMessage = "Username or Password is incorrect!";
//        } else if (logout != null) {
//            errorMessage = "You have been successfully logged out!";
//        } else if (register != null) {
//            errorMessage = "Registration successful. Login with registered credentials!";
//        }
//        model.addAttribute("errorMessage", errorMessage);
//        return "auth/login";
//    }
//
//
//    @RequestMapping(value="/logout", method = RequestMethod.GET)
//    public String logoutPage (HttpServletRequest request, HttpServletResponse response) {
//        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//        if (auth != null){
//            new SecurityContextLogoutHandler().logout(request, response, auth);
//        }
//        return "redirect:/login?logout=true";
//    }
//
//    @GetMapping("/register")
//    public String registerForm(Model model) {
//        model.addAttribute("registration", new RegistrationDto());
//        return "auth/register";
//    }
//
//    @PostMapping("/register")
//    public String registerUser(@ModelAttribute("registration")RegistrationDto registrationDto,
//
//                               BindingResult result, Model model) {
//        if (result.hasErrors()) {
//            return "auth/register";
//        }
//        // basic password confirmation
//        if (!registrationDto.getPassword().equals(registrationDto.getConfirmPassword())) {
//            model.addAttribute("registrationError", "Passwords do not match");
//            return "auth/register";
//        }
//        boolean ok = userService.register(registrationDto);
//        if (ok) {
//            return "redirect:/login?register=true";
//        }
//        model.addAttribute("registrationError", "Username or email already in use");
//        return "auth/register";
//    }
}
