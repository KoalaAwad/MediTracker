package org.springbozo.meditracker.controller;

import org.springbozo.meditracker.model.Role;
import org.springbozo.meditracker.model.User;
import org.springbozo.meditracker.repository.RoleRepository;
import org.springbozo.meditracker.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.StreamSupport;
import org.springframework.transaction.annotation.Transactional;
import org.springbozo.meditracker.service.PatientService;
import org.springbozo.meditracker.repository.MedicineRepository;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PatientService patientService;
    private final MedicineRepository medicineRepository;

    public AdminController(UserRepository userRepository, RoleRepository roleRepository, PatientService patientService, MedicineRepository medicineRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.patientService = patientService;
        this.medicineRepository = medicineRepository;
    }

    @GetMapping({"", "/", "/test"})
    public String adminTest() {
        return "admin/admin-test";
    }

    @GetMapping("/users")
    public String listUsers(Model model) {
        Iterable<User> users = userRepository.findAll();
        model.addAttribute("users", users);
        return "admin/users";
    }

    @GetMapping("/users/{id}/edit")
    public String editUserForm(@PathVariable("id") Integer id, Model model) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return "redirect:/profile/dashboard";
        }
        model.addAttribute("user", userOpt.get());
        model.addAttribute("roles", roleRepository.findAll());
        return "admin/user-edit";
    }

    @PostMapping("/users/{id}/edit")
    @Transactional
    public String updateUserRoles(@PathVariable("id") Integer id,
                                  @RequestParam(value = "roleIds", required = false) Set<Integer> roleIds) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return "redirect:/profile/dashboard";
        }
        User user = userOpt.get();
        // If no roles were submitted, clear the user's roles
        Set<Role> newRoles = new HashSet<>();
        if (roleIds != null && !roleIds.isEmpty()) {
            roleRepository.findAllById(roleIds).forEach(newRoles::add);
        }

        // Determine whether PATIENT role was added or removed
        boolean oldHasPatient = user.getRoles().stream().anyMatch(r -> "PATIENT".equalsIgnoreCase(r.getRoleName()));
        boolean newHasPatient = newRoles.stream().anyMatch(r -> "PATIENT".equalsIgnoreCase(r.getRoleName()));

        // Replace the user's roles with the new set (idempotent)
        user.setRoles(newRoles);
        userRepository.save(user);

        // If PATIENT role was added -> ensure a patient row exists
        if (!oldHasPatient && newHasPatient) {
            // only create if not present
            patientService.getPatientByUserId(Integer.valueOf(user.getId()))
                    .orElseGet(() -> {
                        var p = new org.springbozo.meditracker.model.Patient();
                        p.setUser(user);
                        p.setName(user.getName() != null ? user.getName() : user.getUsername());
                        return patientService.saveOrUpdateForUser(p, Integer.valueOf(user.getId()));
                    });
        }

        // If PATIENT role was removed -> delete patient row
        if (oldHasPatient && !newHasPatient) {
            patientService.deleteByUserId(Integer.valueOf(user.getId()));
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/medicines")
    public String listMedicines(Model model) {
        var meds = medicineRepository.findAll();
        model.addAttribute("medicines", meds);
        return "admin/medicine-list";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable("id") Integer id) {
        userRepository.deleteById(id);
        return "redirect:/profile/dashboard";
    }
}
