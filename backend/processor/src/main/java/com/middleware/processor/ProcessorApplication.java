package com.middleware.processor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
@ComponentScan(basePackages = {
    "com.middleware.processor",
    "com.middleware.common",
    "com.middleware.shared"
})
@EnableJpaRepositories(basePackages = "com.middleware.shared.repository")
@EntityScan(basePackages = "com.middleware.shared.model")
public class ProcessorApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProcessorApplication.class, args);
    }
} 
