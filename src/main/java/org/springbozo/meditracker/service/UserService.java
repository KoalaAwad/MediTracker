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

import java.util.HashSet;
import java.util.List;
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

    public boolean hasAdminRole(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }

        Optional<User> userOpt = findByEmail(email);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        return user.getRoles().stream()
                .anyMatch(role -> Constants.ADMIN_ROLE.equals(role.getRoleName()));
    }

    public List<User> getAllUsers() {
        return (List<User>) userRepository.findAll();
    }

    @Transactional
    public boolean deleteUser(int userId) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return false;
            }

            User user = userOpt.get();
            userRepository.delete(user);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static final int PASSWORD_MIN_LENGTH = 8;
    // BCrypt ignores everything past 72 bytes
    public static final int PASSWORD_MAX_LENGTH = 72;

    /**
     * Returns an error message, or null if the password is acceptable.
     */
    public static String validatePassword(String password) {
        if (password == null || password.length() < PASSWORD_MIN_LENGTH) {
            return "Password must be at least " + PASSWORD_MIN_LENGTH + " characters";
        }
        if (password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > PASSWORD_MAX_LENGTH) {
            return "Password must be at most " + PASSWORD_MAX_LENGTH + " characters";
        }
        return null;
    }

    @Transactional
    public void changePassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * Creates an unverified patient account. Returns null if the input is invalid or the email is taken.
     */
    @Transactional
    public User register(RegistrationDto dto) {
        if (dto == null) return null;

        String email = (dto.getEmail() != null) ? dto.getEmail().trim().toLowerCase() : null;
        String password = dto.getPassword();

        if (email == null || email.isBlank() || validatePassword(password) != null) {
            return null;
        }

        if (userRepository.existsByEmail(email)) {
            return null; // alreay exists
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

        user.setRoles(Set.of(roleRepository.getByRoleName(Constants.USER_ROLE)));

        User saved = userRepository.save(user);

        Patient patient = new Patient();
        patient.setUser(saved);
        patient.setName(saved.getName());
        patient.setActive(true);
        patientRepository.save(patient);

        return saved;
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public List<User> getUsersByRole(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return getAllUsers();
        }
        return userRepository.findByRoleName(roleName);
    }

    /**
     * Build a profile DTO for the given user: account info plus their health profile.
     */
    public org.springbozo.meditracker.DAO.ProfileDto buildProfileDto(User user) {
        if (user == null) return null;

        org.springbozo.meditracker.DAO.ProfileDto profile = new org.springbozo.meditracker.DAO.ProfileDto(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRoles().stream().map(Role::getRoleName).toList()
        );

        var patientOpt = patientRepository.findByUserId(user.getId());
        if (patientOpt.isPresent()) {
            Patient patient = patientOpt.get();
            org.springbozo.meditracker.DAO.PatientProfileDto patientDto = new org.springbozo.meditracker.DAO.PatientProfileDto(
                    patient.getId(),
                    patient.getName(),
                    patient.getGender(),
                    patient.getDateOfBirth(),
                    patient.getPhone(),
                    patient.getAddress(),
                    patient.getBloodType(),
                    patient.getAllergies(),
                    patient.getMedicalHistory(),
                    patient.isActive()
            );
            profile.setPatientProfile(patientDto);
        }

        return profile;
    }
}
