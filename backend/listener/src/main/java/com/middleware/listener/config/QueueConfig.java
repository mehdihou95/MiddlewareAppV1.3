package com.middleware.listener.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;

@Configuration
public class QueueConfig {

    @Value("${queue.thread-pool.core-size:10}")
    private int threadPoolCoreSize;

    @Value("${queue.thread-pool.max-size:20}")
    private int threadPoolMaxSize;

    @Value("${queue.thread-pool.queue-capacity:50}")
    private int threadPoolQueueCapacity;

    // Queue definitions
    @Bean
    public Queue inboundProcessorQueue() {
        return QueueBuilder.durable("inbound.processor")
                .withArgument("x-dead-letter-exchange", "middleware.dlx")
                .withArgument("x-dead-letter-routing-key", "inbound.processor.dlq")
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable("inbound.processor.dlq").build();
    }

    // Exchange definitions
    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange("middleware.direct");
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange("middleware.dlx");
    }

    // Bindings
    @Bean
    public Binding inboundProcessorBinding() {
        return BindingBuilder.bind(inboundProcessorQueue())
                .to(directExchange())
                .with("inbound.processor");
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with("inbound.processor.dlq");
    }

    // Thread pool configuration
    @Bean
    public ThreadPoolTaskExecutor messageProcessorExecutor(MeterRegistry meterRegistry) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threadPoolCoreSize);
        executor.setMaxPoolSize(threadPoolMaxSize);
        executor.setQueueCapacity(threadPoolQueueCapacity);
        executor.setThreadNamePrefix("msg-processor-");
        executor.initialize();

        // Add metrics
        ExecutorServiceMetrics.monitor(
            meterRegistry,
            executor.getThreadPoolExecutor(),
            "message.processor.thread.pool"
        );

        return executor;
    }

    // Enhanced RabbitTemplate with metrics
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MeterRegistry meterRegistry) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        
        // Add metrics for message publishing
        meterRegistry.gauge("rabbitmq.messages.published", rabbitTemplate, 
            template -> template.getMessageConverter().getClass().getSimpleName().length());
        
        return rabbitTemplate;
    }
} 