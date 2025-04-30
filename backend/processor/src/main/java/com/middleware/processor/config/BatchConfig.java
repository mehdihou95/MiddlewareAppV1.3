package com.middleware.processor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;

@Configuration
public class BatchConfig {

    @Value("${batch.core-pool-size:5}")
    private int corePoolSize;

    @Value("${batch.max-pool-size:10}")
    private int maxPoolSize;

    @Value("${batch.queue-capacity:25}")
    private int queueCapacity;

    @Value("${batch.thread-name-prefix:BatchProcessor-}")
    private String threadNamePrefix;

    @Bean(name = "batchTaskExecutor")
    public Executor batchTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }
} 