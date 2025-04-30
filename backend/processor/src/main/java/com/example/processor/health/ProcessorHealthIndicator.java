package com.example.processor.health;

import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

@Component
public class ProcessorHealthIndicator extends AbstractHealthIndicator {

    private final RabbitAdmin rabbitAdmin;
    private final RabbitTemplate rabbitTemplate;

    public ProcessorHealthIndicator(RabbitAdmin rabbitAdmin, RabbitTemplate rabbitTemplate) {
        this.rabbitAdmin = rabbitAdmin;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        try {
            // Check RabbitMQ connection
            rabbitTemplate.execute(channel -> {
                channel.queueDeclarePassive("health.check");
                return null;
            });

            // Get queue information
            var queueInfo = rabbitAdmin.getQueueInfo("inbound.processor");
            
            builder.up()
                .withDetail("rabbitmq", "connected")
                .withDetail("queue", queueInfo != null ? "exists" : "not found")
                .withDetail("messageCount", queueInfo != null ? queueInfo.getMessageCount() : 0)
                .withDetail("consumerCount", queueInfo != null ? queueInfo.getConsumerCount() : 0);
        } catch (Exception e) {
            builder.down()
                .withException(e)
                .withDetail("error", "Failed to check processor health");
        }
    }
} 