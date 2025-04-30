package com.middleware.processor.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DeadLetterConfig {
    public static final String DLX_NAME = "dlx";
    public static final String DLQ_NAME = "dlq";
    public static final String DLQ_ROUTING_KEY = "dlq";
    
    @Value("${rabbitmq.dlq.ttl:86400000}") // 24 hours default
    private long dlqTtl;
    
    @Value("${rabbitmq.dlq.max-length:10000}")
    private int dlqMaxLength;
    
    @Value("${rabbitmq.dlq.max-length-bytes:104857600}") // 100MB default
    private int dlqMaxLengthBytes;
    
    @Value("${rabbitmq.dlq.max-priority:10}")
    private int dlqMaxPriority;

    @Bean
    public DirectExchange dlx() {
        return new DirectExchange(DLX_NAME, true, false);
    }

    @Bean
    public Queue dlq() {
        return QueueBuilder.durable(DLQ_NAME)
                .withArgument("x-message-ttl", dlqTtl)
                .withArgument("x-max-length", dlqMaxLength)
                .withArgument("x-max-length-bytes", dlqMaxLengthBytes)
                .withArgument("x-max-priority", dlqMaxPriority)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", DLQ_NAME + ".retry")
                .build();
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(dlq())
                .to(dlx())
                .with(DLQ_ROUTING_KEY);
    }

    @Bean
    public Queue retryQueue() {
        return QueueBuilder.durable(DLQ_NAME + ".retry")
                .withArgument("x-message-ttl", dlqTtl / 2) // Half of DLQ TTL
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public MessageRecoverer messageRecoverer(RabbitTemplate rabbitTemplate) {
        return new RepublishMessageRecoverer(rabbitTemplate, DLX_NAME, DLQ_ROUTING_KEY);
    }
} 
