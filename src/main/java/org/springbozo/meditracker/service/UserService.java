package org.springbozo.meditracker.service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springbozo.meditracker.DAO.RegistrationDto;
import org.springbozo.meditracker.constants.Constants;
import org.springbozo.meditracker.model.Role;
import org.springbozo.meditracker.model.User;
import org.springbozo.meditracker.model.Patient;
import org.springbozo.meditracker.repository.RoleRepository;
import org.springbozo.meditracker.repository.UserRepository;
import org.springbozo.meditracker.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
        return userRepository.findByEmail(email).isPresent();
    }

    public boolean usernameExists(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean savePerson(User user){
        userRepository.save(user);
        return true;
    }

    public Optional<User> findByUsernameOrEmail(String identifier) {
        if (identifier == null) return Optional.empty();
        Optional<User> byUsername = userRepository.findByUsername(identifier);
        if (byUsername.isPresent()) return byUsername;
        return userRepository.findByEmail(identifier.toLowerCase());
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findById(Integer id) {
        return userRepository.findById(id);
    }

    public boolean register(RegistrationDto registrationDto) {
        // check email and username
        String normalizedEmail = registrationDto.getEmail() != null ? registrationDto.getEmail().trim().toLowerCase() : null;
        String username = registrationDto.getUsername() != null ? registrationDto.getUsername().trim() : null;
        if (normalizedEmail == null || username == null) {
            return false;
        }
        if (userRepository.existsByEmail(normalizedEmail) || userRepository.existsByUsername(username)) {
            return false; // conflict
        }

        User user = new User();
        user.setPassword(passwordEncoder.encode(registrationDto.getPassword()));
        user.setName(registrationDto.getName());
        user.setEmail(normalizedEmail);
        // set username from DTO
        user.setUsername(username);

        // assign PATIENT role by default
        Role patientRole = roleRepository.getByRoleName(Constants.PATIENT_ROLE);
        if (patientRole == null) {
            patientRole = new Role();
            patientRole.setRoleName(Constants.PATIENT_ROLE);
            roleRepository.save(patientRole);
        }
        user.setRoles(Set.of(patientRole));

        // save user
        User saved = userRepository.save(user);

        // create corresponding Patient row and link to user
        Patient patient = new Patient();
        patient.setUser(saved);
        // set patient.name to user's display name to satisfy NOT NULL constraint
        patient.setName(saved.getName());
        // set defaults if necessary
        patientRepository.save(patient);

        return true;
    }


    public List<User> getAllUsers() {
        return (List<User>) userRepository.findAll();
    }
}
