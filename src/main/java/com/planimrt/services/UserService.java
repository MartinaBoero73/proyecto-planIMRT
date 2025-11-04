package com.planimrt.services;

import com.planimrt.DTOs.UserDTO;
import com.planimrt.model.User;
import com.planimrt.model.UserRole;
import com.planimrt.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    public UserDTO getLoggedUserDTO() {
        String username = getAuthenticatedUsername();
        return getUserDTOByUsername(username);
    }

    public Optional<User> getLoggedUser() {
        String username = getAuthenticatedUsername();
        return userRepository.findByUsername(username);
    }

    public Long getLoggedUserId() {
        return getLoggedUserDTO().getId();
    }

    private String getAuthenticatedUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        } else {
            return principal.toString();
        }
    }

    public List<User> getAllUsers() {
        return (List<User>) userRepository.findAll();
    }

    public Optional<Optional<User>> getByUsername(String username) {
        return Optional.ofNullable(userRepository.findByUsername(username));
    }

    public UserDTO getUserDTOByUsername(String username) {
        Optional<User> u = userRepository.findByUsername(username);

        return new UserDTO(u.get().getId(), u.get().getUsername(), u.get().getRole());
    }

    public void registerUser(String username, String rawPassword, UserRole role) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("El usuario ya existe: " + username);
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setActive(true);

        userRepository.save(user);
    }
}
