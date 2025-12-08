package org.springbozo.meditracker.service;

import org.springbozo.meditracker.DAO.RegistrationDto;
import org.springbozo.meditracker.constants.Constants;
import org.springbozo.meditracker.model.Patient;
import org.springbozo.meditracker.model.Role;
import org.springbozo.meditracker.model.User;
import org.springbozo.meditracker.repository.DoctorRepository;
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
    private DoctorRepository doctorRepository;
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

    public List<Role> getAllRoles() {
        return (List<Role>) roleRepository.findAll();
    }

    @Transactional
    public boolean updateUserRoles(int userId, List<String> roleNames) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return false;
            }

            User user = userOpt.get();
            Set<Role> newRoles = new HashSet<>();

            for (String roleName : roleNames) {
                Role role = roleRepository.getByRoleName(roleName);
                if (role == null) {
                    return false;
                }
                newRoles.add(role);
            }

            Set<String> existingRoleNames = user.getRoles().stream().map(Role::getRoleName).collect(java.util.stream.Collectors.toSet());
            Set<String> requestedRoleNames = new java.util.HashSet<>(roleNames);

            // the new roles that i give you
            Set<String> added = new java.util.HashSet<>(requestedRoleNames);
            added.removeAll(existingRoleNames);

            // the roles that i take away from you
            Set<String> removed = new java.util.HashSet<>(existingRoleNames);
            removed.removeAll(requestedRoleNames);

            // Apply role set
            user.setRoles(newRoles);
            userRepository.save(user);

            // Handle entity mapping for patient/doctor roles using soft-delete (active flag)
            // If patient role added -> create or reactivate Patient
            if (added.contains(Constants.PATIENT_ROLE)) {
                Optional<Patient> patientOpt = patientRepository.findByUserId(user.getId());
                if (patientOpt.isPresent()) {
                    // Found existing patient (possibly inactive) -> reactivate if needed
                    Patient patient = patientOpt.get();
                    if (!patient.isActive()) {
                        patient.setActive(true);
                        patientRepository.save(patient);
                    }
                } else {
                    // No existing patient record -> create new one
                    Patient patient = new Patient();
                    patient.setUser(user);
                    patient.setName(user.getName());
                    patient.setActive(true);
                    patientRepository.save(patient);
                    user.setPatient(patient);
                    userRepository.save(user);
                }
            }

            // If patient role removed -> mark inactive but keep data
            if (removed.contains(Constants.PATIENT_ROLE)) {
                Patient patient = patientRepository.findByUserId(user.getId()).orElse(null);
                if (patient != null && patient.isActive()) {
                    patient.setActive(false);
                    patientRepository.save(patient);
                }
            }

            // If doctor role added -> create or reactivate Doctor record
            if (added.contains(Constants.DOCTOR_ROLE)) {
                Optional<org.springbozo.meditracker.model.Doctor> doctorOpt = doctorRepository.findByUserId(user.getId());
                if (doctorOpt.isPresent()) {
                    // Found existing doctor (possibly inactive) -> reactivate if needed
                    org.springbozo.meditracker.model.Doctor doctor = doctorOpt.get();
                    if (!doctor.isActive()) {
                        doctor.setActive(true);
                        doctorRepository.save(doctor);
                    }
                } else {
                    // No existing doctor record -> create new one
                    org.springbozo.meditracker.model.Doctor doctor = new org.springbozo.meditracker.model.Doctor();
                    doctor.setUser(user);
                    // derive firstName and lastName from user's name (DB requires last_name NOT NULL)
                    String fullName = user.getName();
                    if (fullName == null || fullName.isBlank()) {
                        // leave names null; doctor profile optional
                        doctor.setFirstName(null);
                        doctor.setLastName(null);
                    } else {
                        String[] parts = fullName.trim().split("\\s+", 2);
                        doctor.setFirstName(parts[0]);
                        doctor.setLastName(parts.length > 1 ? parts[1] : null);
                    }
                    doctor.setActive(true);
                    doctorRepository.save(doctor);
                }
            }

            // If doctor role removed -> mark inactive but preserve data
            if (removed.contains(Constants.DOCTOR_ROLE)) {
                var docOpt = doctorRepository.findByUserId(user.getId());
                if (docOpt.isPresent()) {
                    var d = docOpt.get();
                    if (d.isActive()) {
                        d.setActive(false);
                        doctorRepository.save(d);
                    }
                }
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
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

    @Transactional
    public boolean register(RegistrationDto dto) {
        if (dto == null) return false;

        String email = (dto.getEmail() != null) ? dto.getEmail().trim().toLowerCase() : null;
        String password = dto.getPassword();

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return false;
        }

        if (userRepository.existsByEmail(email)) {
            return false; // alreay exists
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
        patient.setActive(true);
        patientRepository.save(patient);

        return true;
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public boolean hasDoctorRole(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }

        Optional<User> userOpt = findByEmail(email);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        return user.getRoles().stream()
                .anyMatch(role -> Constants.DOCTOR_ROLE.equals(role.getRoleName()));
    }

    public List<User> getUsersByRole(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return getAllUsers();
        }
        return userRepository.findByRoleName(roleName);
    }
    public List<User> getUsersWithOnlyRole(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return List.of();
        }
        return userRepository.findByOnlyRoleName(roleName);
    }

    public List<User> getUsersFilteredByRole(String roleName, boolean only) {
        return only ? getUsersWithOnlyRole(roleName) : getUsersByRole(roleName);
    }

    /**
     * Build a profile DTO for the given user including roles and role-specific data (patient, doctor).
     */
    public org.springbozo.meditracker.DAO.ProfileDto buildProfileDto(User user) {
        if (user == null) return null;

        org.springbozo.meditracker.DAO.ProfileDto profile = new org.springbozo.meditracker.DAO.ProfileDto(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRoles().stream().map(Role::getRoleName).toList()
        );

        // Add patient profile if user has PATIENT role
        if (user.getRoles().stream().anyMatch(r -> Constants.PATIENT_ROLE.equals(r.getRoleName()))) {
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
        }

        // Add doctor profile if user has DOCTOR role
        if (user.getRoles().stream().anyMatch(r -> Constants.DOCTOR_ROLE.equals(r.getRoleName()))) {
            var doctorOpt = doctorRepository.findByUserId(user.getId());
            if (doctorOpt.isPresent()) {
                org.springbozo.meditracker.model.Doctor doctor = doctorOpt.get();
                org.springbozo.meditracker.DAO.DoctorProfileDto doctorDto = new org.springbozo.meditracker.DAO.DoctorProfileDto(
                        doctor.getId(),
                        doctor.getFirstName(),
                        doctor.getLastName(),
                        doctor.getSpecialization(),
                        doctor.getLicenseNumber(),
                        doctor.getPhone(),
                        doctor.getClinicAddress(),
                        doctor.isActive()
                );
                profile.setDoctorProfile(doctorDto);
            }
        }

        return profile;
    }
}
