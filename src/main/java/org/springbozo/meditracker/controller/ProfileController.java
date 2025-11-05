package org.springbozo.meditracker.controller;

import org.springbozo.meditracker.model.Patient;
import org.springbozo.meditracker.model.User;
import org.springbozo.meditracker.model.Doctor;
import org.springbozo.meditracker.service.PatientService;
import org.springbozo.meditracker.service.UserService;
import org.springbozo.meditracker.repository.DoctorRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class ProfileController {

    private final PatientService patientService;
    private final UserService userService;
    private final DoctorRepository doctorRepository;

    public ProfileController(PatientService patientService, UserService userService, DoctorRepository doctorRepository) {
        this.patientService = patientService;
        this.userService = userService;
        this.doctorRepository = doctorRepository;
    }

    @GetMapping({"/profile/dashboard", "dashboard"})
    public String dashboard() {
        return "profile/dashboard";
    }

    // SHOW profile (read-only). Dashboard links to /profile which renders this view.
    @GetMapping({"/profile","/profile/view"})
    public String profileView(Authentication authentication, Model model) {
        if (authentication == null) return "redirect:/login";
        String identifier = authentication.getName();
        Optional<User> userOpt = userService.findByUsernameOrEmail(identifier);
        if (userOpt.isEmpty()) return "redirect:/login";
        User user = userOpt.get();

        boolean isPatient = user.getRoles().stream().anyMatch(r -> "PATIENT".equalsIgnoreCase(r.getRoleName()));
        if (isPatient) {
            Optional<Patient> patientOpt = patientService.getPatientByUserId(Integer.valueOf(user.getId()));
            Patient patient = patientOpt.orElse(null);
            model.addAttribute("patient", patient);
            model.addAttribute("user", user);
            // roles set
            Set<String> roles = user.getRoles().stream().map(r -> r.getRoleName()).collect(Collectors.toSet());
            model.addAttribute("roles", roles);
            // also load doctor if they have the DOCTOR role
            if (roles.contains("DOCTOR")) {
                Optional<Doctor> doctorOpt = doctorRepository.findByUserId(user.getId());
                doctorOpt.ifPresent(d -> model.addAttribute("doctor", d));
            }
            return "profile/profile-view";
        }

        // default: show user and roles
        Set<String> roles = user.getRoles().stream().map(r -> r.getRoleName()).collect(Collectors.toSet());
        model.addAttribute("roles", roles);
        model.addAttribute("user", user);
        return "profile/profile-view";
    }

    @GetMapping("/profile/edit")
    public String editProfile(Authentication authentication, Model model) {
        if (authentication == null) return "redirect:/login";
        String username = authentication.getName();
        Optional<User> userOpt = userService.findByUsernameOrEmail(username);
        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }
        User user = userOpt.get();

        // if user has PATIENT role, show patient edit
        boolean isPatient = user.getRoles().stream().anyMatch(r -> "PATIENT".equalsIgnoreCase(r.getRoleName()));
        if (isPatient) {
            Optional<Patient> patientOpt = patientService.getPatientByUserId(Integer.valueOf(user.getId()));
            Patient patient = patientOpt.orElseGet(() -> {
                Patient p = new Patient();
                p.setUser(user);
                return p;
            });
            model.addAttribute("patient", patient);
            return "profile/patient-edit";
        }

        // default: show user entity test page
        model.addAttribute("user", user);
        return "profile/user-entity-test";
    }

    @PostMapping("/profile/edit")
    public String saveProfile(@ModelAttribute("patient") Patient patient, Authentication authentication) {
        if (authentication == null) return "redirect:/login";
        String username = authentication.getName();
        Optional<User> userOpt = userService.findByUsernameOrEmail(username);
        if (userOpt.isEmpty()) return "redirect:/login";

        User user = userOpt.get();

        // save or update (upsert) to prevent duplicate user_id insert
        patientService.saveOrUpdateForUser(patient, Integer.valueOf(user.getId()));
        return "redirect:/profile";
    }
}
