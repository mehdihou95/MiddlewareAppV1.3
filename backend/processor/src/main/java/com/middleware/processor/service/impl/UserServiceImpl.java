package com.middleware.processor.service.impl;

import com.middleware.shared.model.User;
import com.middleware.shared.repository.UserRepository;
import com.middleware.processor.service.interfaces.UserService;
import com.middleware.processor.annotation.AuditLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Primary
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Override
    public Page<User> getUsers(Pageable pageable, String searchTerm, Boolean enabled) {
        if (searchTerm != null && !searchTerm.isEmpty()) {
            return userRepository.findByUsernameContainingIgnoreCase(searchTerm, pageable);
        }
        if (enabled != null) {
            return userRepository.findByEnabled(enabled, pageable);
        }
        return userRepository.findAll(pageable);
    }

    @Override
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    @AuditLog("Create user")
    public User createUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    @Override
    @AuditLog("Update user")
    public Optional<User> updateUser(Long id, User userDetails) {
        return userRepository.findById(id)
            .map(existingUser -> {
                existingUser.setUsername(userDetails.getUsername());
                if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
                    existingUser.setPassword(passwordEncoder.encode(userDetails.getPassword()));
                }
                existingUser.setEnabled(userDetails.isEnabled());
                existingUser.setRoles(userDetails.getRoles());
                return userRepository.save(existingUser);
            });
    }

    @Override
    @AuditLog("Delete user")
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    @Override
    public boolean changePassword(Long id, String oldPassword, String newPassword) {
        return userRepository.findById(id)
            .map(user -> {
                if (passwordEncoder.matches(oldPassword, user.getPassword())) {
                    user.setPassword(passwordEncoder.encode(newPassword));
                    userRepository.save(user);
                    return true;
                }
                return false;
            })
            .orElse(false);
    }

    @Override
    public void resetPassword(Long id, String newPassword) {
        userRepository.findById(id).ifPresent(user -> {
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
        });
    }

    @Override
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return org.springframework.security.core.userdetails.User
            .withUsername(user.getUsername())
            .password(user.getPassword())
            .authorities(user.getRoles().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList()))
            .build();
    }
} 
