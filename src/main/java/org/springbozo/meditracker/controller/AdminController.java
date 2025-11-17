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

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public AdminController(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
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
    public String updateUserRoles(@PathVariable("id") Integer id, @RequestParam("roleIds") Set<Integer> roleIds) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return "redirect:/profile/dashboard";
        }
        User user = userOpt.get();
        Set<Role> newRoles = new HashSet<>(roleRepository.findAllById(roleIds));
        user.setRoles(newRoles);
        userRepository.save(user);
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable("id") Integer id) {
        userRepository.deleteById(id);
        return "redirect:/profile/dashboard";
    }
}
