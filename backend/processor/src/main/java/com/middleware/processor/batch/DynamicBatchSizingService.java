package com.middleware.processor.batch;

import com.middleware.processor.metrics.ProcessingMetrics;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class DynamicBatchSizingService {

    private final RabbitAdmin rabbitAdmin;
    private final ProcessingMetrics processingMetrics;
    private final AtomicInteger currentBatchSize;
    private final int minBatchSize;
    private final int maxBatchSize;
    private final int queueDepthThreshold;
    private final int adjustmentStep;

    public DynamicBatchSizingService(RabbitAdmin rabbitAdmin,
                                   ProcessingMetrics processingMetrics,
                                   @Value("${batch.min-size:10}") int minBatchSize,
                                   @Value("${batch.max-size:100}") int maxBatchSize,
                                   @Value("${batch.queue-depth-threshold:1000}") int queueDepthThreshold,
                                   @Value("${batch.adjustment-step:10}") int adjustmentStep) {
        this.rabbitAdmin = rabbitAdmin;
        this.processingMetrics = processingMetrics;
        this.minBatchSize = minBatchSize;
        this.maxBatchSize = maxBatchSize;
        this.queueDepthThreshold = queueDepthThreshold;
        this.adjustmentStep = adjustmentStep;
        this.currentBatchSize = new AtomicInteger(minBatchSize);
    }

    @Scheduled(fixedRate = 30000)
    public void adjustBatchSize() {
        int queueDepth = getQueueDepth();
        double systemLoad = getSystemLoad();
        
        int newBatchSize = calculateNewBatchSize(queueDepth, systemLoad);
        currentBatchSize.set(newBatchSize);
        
        processingMetrics.getQueueMetrics().monitorQueueDepth();
    }

    private int getQueueDepth() {
        var queueInfo = rabbitAdmin.getQueueInfo("inbound.processor");
        return queueInfo != null ? queueInfo.getMessageCount() : 0;
    }

    private double getSystemLoad() {
        return processingMetrics.getRegistry().get("system.cpu.usage")
            .gauge()
            .value();
    }

    private int calculateNewBatchSize(int queueDepth, double systemLoad) {
        int currentSize = currentBatchSize.get();
        
        if (queueDepth > queueDepthThreshold && systemLoad < 0.7) {
            // Increase batch size if queue is deep and system has capacity
            return Math.min(currentSize + adjustmentStep, maxBatchSize);
        } else if (queueDepth < queueDepthThreshold / 2 || systemLoad > 0.8) {
            // Decrease batch size if queue is shallow or system is under load
            return Math.max(currentSize - adjustmentStep, minBatchSize);
        }
        
        return currentSize;
    }

    public int getCurrentBatchSize() {
        return currentBatchSize.get();
    }
} 
