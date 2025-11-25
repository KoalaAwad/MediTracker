package org.springbozo.meditracker.service;

import org.springbozo.meditracker.DAO.RegistrationDto;
import org.springbozo.meditracker.constants.Constants;
import org.springbozo.meditracker.model.Patient;
import org.springbozo.meditracker.model.Role;
import org.springbozo.meditracker.model.User;
import org.springbozo.meditracker.repository.PatientRepository;
import org.springbozo.meditracker.repository.RoleRepository;
import org.springbozo.meditracker.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public boolean emailExists(String email) {
        if (email == null) return false;
        return userRepository.findByEmail(email.toLowerCase()).isPresent();
    }

    public Optional<User> findByEmail(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByEmail(identifier.trim().toLowerCase());
    }

    @Transactional
    public boolean register(RegistrationDto dto) {
        if (dto == null) return false;

        String email = (dto.getEmail() != null) ? dto.getEmail().trim().toLowerCase() : null;
        String password = dto.getPassword();

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return false;
        }

        if (userRepository.existsByEmail(email)) {
            return false; // already exists
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));

        String name = dto.getName();
        if (name == null || name.isBlank()) {
            // derive simple display name from email local part
            name = email.contains("@") ? email.substring(0, email.indexOf('@')) : "User";
        }
        user.setName(name);

        Role patientRole = roleRepository.getByRoleName(Constants.PATIENT_ROLE);
        if (patientRole == null) {
            patientRole = new Role();
            patientRole.setRoleName(Constants.PATIENT_ROLE);
            roleRepository.save(patientRole);
        }
        user.setRoles(Set.of(patientRole));

        User saved = userRepository.save(user);

        Patient patient = new Patient();
        patient.setUser(saved);
        patient.setName(saved.getName());
        patientRepository.save(patient);

        return true;
    }
}
