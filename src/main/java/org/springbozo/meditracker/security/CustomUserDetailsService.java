package org.springbozo.meditracker.security;

import org.springbozo.meditracker.model.Role;
import org.springbozo.meditracker.model.User;
import org.springbozo.meditracker.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        // identifier may be username or email
        var userOpt = userRepository.findByUsernameOrEmail(identifier, identifier);
        User user = userOpt.orElseThrow(() -> new UsernameNotFoundException("User not found: " + identifier));

        Set<GrantedAuthority> authorities = user.getRoles().stream()
                .map(Role::getRoleName)
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .collect(Collectors.toSet());

        // Spring Security's User class: username must be non-null
        String usernameForPrincipal = user.getUsername() != null ? user.getUsername() : user.getEmail();

        return org.springframework.security.core.userdetails.User
                .withUsername(usernameForPrincipal)
                .password(user.getPassword())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }
}

