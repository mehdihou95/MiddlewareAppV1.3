package com.middleware.processor.config;

import com.middleware.shared.config.SharedConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration class for the processor module.
 * Extends SharedConfig to inherit common configurations and adds processor-specific settings.
 */
@Configuration
@Import(com.middleware.shared.config.JpaConfig.class)
public class ProcessorConfig extends SharedConfig {
    
    @Value("${processor.thread-pool.core-size:10}")
    private int corePoolSize;
    
    @Value("${processor.thread-pool.max-size:20}")
    private int maxPoolSize;
    
    @Value("${processor.thread-pool.queue-capacity:100}")
    private int queueCapacity;
    
    @Value("${processor.thread-pool.keep-alive-seconds:60}")
    private int keepAliveSeconds;
    
    @Value("${processor.batch.size:100}")
    private int batchSize;
    
    @Value("${processor.batch.timeout:30000}")
    private long batchTimeout;
    
    /**
     * Thread pool executor for processor operations.
     */
    @Bean(name = "processorTaskExecutor")
    public Executor processorTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setThreadNamePrefix("processor-");
        executor.initialize();
        return executor;
    }
    
    /**
     * Get the configured batch size.
     */
    public int getBatchSize() {
        return batchSize;
    }
    
    /**
     * Get the configured batch timeout in milliseconds.
     */
    public long getBatchTimeout() {
        return batchTimeout;
    }
} 