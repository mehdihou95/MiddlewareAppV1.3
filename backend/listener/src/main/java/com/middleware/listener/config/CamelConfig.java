package com.middleware.listener.config;

import org.apache.camel.CamelContext;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CamelConfig {
    @Bean
    CamelContextConfiguration contextConfiguration() {
        return new CamelContextConfiguration() {
            @Override
            public void beforeApplicationStart(CamelContext context) {
                // Configure Camel context before start
                context.setTracing(false);
                context.setMessageHistory(false);
                context.setLoadTypeConverters(true);
            }

            @Override
            public void afterApplicationStart(CamelContext camelContext) {
                // Post-start configuration if needed
            }
        };
    }
} 