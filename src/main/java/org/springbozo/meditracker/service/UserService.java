package org.springbozo.meditracker.service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springbozo.meditracker.DAO.RegistrationDto;
import org.springbozo.meditracker.constants.Constants;
import org.springbozo.meditracker.model.Role;
import org.springbozo.meditracker.model.User;
import org.springbozo.meditracker.repository.RoleRepository;
import org.springbozo.meditracker.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;




    public boolean emailExists(String email) {

        User user = userRepository.findByEmail(email);
        if (user == null){
            return false;
        }
        return true;
    }

    public boolean savePerson(User user){
        userRepository.save(user);
        return true;
    }

    public boolean register(RegistrationDto registrationDto) {
        User user = new User();
        boolean isSaved = false;
        Role role = roleRepository.getByRoleName(Constants.ADMIN_ROLE);
        user.setRoles(Set.of(role));
        user.setPassword(passwordEncoder.encode(registrationDto.getPassword()));
        user.setName(registrationDto.getName());
        user.setEmail(registrationDto.getEmail());
        isSaved = savePerson(user);
        return isSaved;
    }


    public List<User> getAllUsers() {
        return (List<User>) userRepository.findAll();
    }
}



