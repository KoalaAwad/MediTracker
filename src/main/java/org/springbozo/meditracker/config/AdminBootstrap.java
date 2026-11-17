package org.springbozo.meditracker.config;

import lombok.extern.slf4j.Slf4j;
import org.springbozo.meditracker.constants.Constants;
import org.springbozo.meditracker.model.Patient;
import org.springbozo.meditracker.model.Role;
import org.springbozo.meditracker.model.User;
import org.springbozo.meditracker.repository.PatientRepository;
import org.springbozo.meditracker.repository.RoleRepository;
import org.springbozo.meditracker.repository.UserRepository;
import org.springbozo.meditracker.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * Creates the first admin account from ADMIN_EMAIL / ADMIN_PASSWORD when no admin exists yet.
 */
@Component
@Slf4j
public class AdminBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    public AdminBootstrap(UserRepository userRepository, RoleRepository roleRepository, PatientRepository patientRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.patientRepository = patientRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!userRepository.findByRoleName(Constants.ADMIN_ROLE).isEmpty()) {
            return;
        }
        if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            log.warn("No admin account exists. Set ADMIN_EMAIL and ADMIN_PASSWORD to create one on startup.");
            return;
        }
        String passwordError = UserService.validatePassword(adminPassword);
        if (passwordError != null) {
            log.error("ADMIN_PASSWORD rejected: {}", passwordError);
            return;
        }

        String email = adminEmail.trim().toLowerCase();
        User user = userRepository.findByEmail(email).orElseGet(User::new);
        boolean isNew = user.getEmail() == null;
        if (isNew) {
            user.setEmail(email);
            user.setName("Admin");
            user.setPassword(passwordEncoder.encode(adminPassword));
        }
        user.setEmailVerified(true);
        // an existing account keeps its own password and roles, and gains admin + patient
        Set<Role> roles = new HashSet<>(user.getRoles());
        roles.add(roleRepository.getByRoleName(Constants.ADMIN_ROLE));
        roles.add(roleRepository.getByRoleName(Constants.USER_ROLE));
        user.setRoles(roles);
        User saved = userRepository.save(user);

        if (patientRepository.findByUserId(saved.getId()).isEmpty()) {
            Patient patient = new Patient();
            patient.setUser(saved);
            patient.setName(saved.getName());
            patient.setActive(true);
            patientRepository.save(patient);
        }
        log.info("{} admin account {}", isNew ? "Created" : "Promoted existing", email);
    }
}
