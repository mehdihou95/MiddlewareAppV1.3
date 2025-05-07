package com.middleware.listener;

import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
    "com.middleware.listener",
    "com.middleware.shared",
    "com.middleware.shared.security"
})
@EntityScan(basePackages = {"com.middleware.shared.model"})
@EnableJpaRepositories(basePackages = {"com.middleware.shared.repository"})
@Import({CamelAutoConfiguration.class})
@EnableScheduling
public class ListenerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ListenerApplication.class, args);
    }
} 