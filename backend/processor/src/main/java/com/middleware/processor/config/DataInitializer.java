package com.middleware.processor.config;

import com.middleware.shared.model.User;
import com.middleware.shared.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@Transactional
public class DataInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Create admin user with ADMIN role
        if (!userRepository.existsByUsername("admin")) {
            User adminUser = new User();
            adminUser.setUsername("admin");
            adminUser.setPassword(passwordEncoder.encode("admin"));
            adminUser.setEmail("admin@example.com");
            adminUser.setRoles(new ArrayList<>(List.of("ADMIN")));
            User savedAdmin = userRepository.save(adminUser);
            logger.info("Created admin user with ADMIN role, id: {}", savedAdmin.getId());
        }
        
        // Create regular user with USER role
        if (!userRepository.existsByUsername("user")) {
            User regularUser = new User();
            regularUser.setUsername("user");
            regularUser.setPassword(passwordEncoder.encode("user"));
            regularUser.setEmail("user@example.com");
            regularUser.setRoles(new ArrayList<>(List.of("USER")));
            User savedUser = userRepository.save(regularUser);
            logger.info("Created regular user with USER role, id: {}", savedUser.getId());
        }
    }
} 
