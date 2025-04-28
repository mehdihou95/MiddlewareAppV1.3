package com.middleware.listener.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import com.middleware.shared.security.config.SecurityConfig;

@Configuration
@Order(1)  // Ensure this configuration takes precedence
@Import(SecurityConfig.class)
@ComponentScan(basePackages = {
    "com.middleware.shared.security",
    "com.middleware.shared.security.service",
    "com.middleware.shared.security.filter"
})
public class ListenerSecurityConfig {
    // The shared security configuration will be imported and used
} 