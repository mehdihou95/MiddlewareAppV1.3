package com.middleware.processor.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.core.AcknowledgeMode;

@Configuration
public class RabbitMQConfig {
    
    @Value("${rabbitmq.queue.inbound.processor}")
    private String inboundProcessorQueue;
    
    @Bean
    public Queue inboundProcessorQueue() {
        return new Queue(inboundProcessorQueue, true); // durable queue
    }
    
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setConcurrentConsumers(1);  // Start with single consumer
        factory.setMaxConcurrentConsumers(3); // Allow scaling to 3
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        return factory;
    }
} 