package com.example.processor.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class QueueMonitoringService {

    private final RabbitAdmin rabbitAdmin;
    private final MeterRegistry meterRegistry;
    private final Queue inboundQueue;

    public QueueMonitoringService(RabbitAdmin rabbitAdmin, 
                                MeterRegistry meterRegistry,
                                Queue inboundQueue) {
        this.rabbitAdmin = rabbitAdmin;
        this.meterRegistry = meterRegistry;
        this.inboundQueue = inboundQueue;
    }

    @Scheduled(fixedRate = 5000)
    public void monitorQueueDepth() {
        int messageCount = rabbitAdmin.getQueueInfo(inboundQueue.getName()).getMessageCount();
        meterRegistry.gauge("rabbitmq.queue.depth", 
            Tags.of("queue", inboundQueue.getName()),
            messageCount);
    }
} 