package com.middleware.processor.listener;

import com.middleware.processor.service.interfaces.XmlProcessorService;
import com.middleware.shared.repository.InterfaceRepository;
import com.middleware.processor.exception.ValidationException;
import com.middleware.shared.model.ProcessedFile;
import com.middleware.shared.model.Interface;
import com.middleware.processor.model.MessageContent;
import com.middleware.processor.metrics.ProcessingMetrics;
import lombok.RequiredArgsConstructor;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.rabbitmq.client.Channel;
import java.io.*;
import java.nio.file.Files;
import com.middleware.processor.service.BatchProcessorService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@Component
@RequiredArgsConstructor
public class InboundMessageListener {
    
    private final XmlProcessorService xmlProcessorService;
    private final InterfaceRepository interfaceRepository;
    private final BatchProcessorService batchProcessorService;
    private final ProcessingMetrics processingMetrics;
    private final RabbitTemplate rabbitTemplate;
    
    @Value("${batch.size:100}")
    private int batchSize;
    
    @Transactional
    @RabbitListener(
        queues = "${spring.rabbitmq.queue.inbound.processor}",
        containerFactory = "rabbitListenerContainerFactory"
    )
    public void processMessage(Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        
        try {
            // 1. Extract and validate message content
            MessageContent content = extractMessageContent(message);
            validateContent(content);
            
            // 2. Determine priority based on client/interface
            String priority = determinePriority(content);
            
            // 3. Create new message with priority
            MessageProperties properties = new MessageProperties();
            properties.setPriority(getPriorityValue(priority));
            Message prioritizedMessage = new Message(message.getBody(), properties);
            
            // 4. Route to appropriate priority queue
            rabbitTemplate.send("middleware.priority", priority, prioritizedMessage);
            
            // 5. Acknowledge original message
            channel.basicAck(deliveryTag, false);
            
        } catch (ValidationException e) {
            handleValidationError(e, deliveryTag, channel);
            processingMetrics.getCounters().processingErrors("validation").increment();
        } catch (Exception e) {
            handleProcessingError(e, deliveryTag, channel);
            processingMetrics.getCounters().processingErrors("processing").increment();
        }
    }
    
    private String determinePriority(MessageContent content) {
        // Example priority determination based on client/interface
        // This should be customized based on your business rules
        if (content.getInterfaceEntity().isHighPriority()) {
            return "high";
        } else if (content.getInterfaceEntity().isNormalPriority()) {
            return "normal";
        } else {
            return "low";
        }
    }
    
    private int getPriorityValue(String priority) {
        switch (priority) {
            case "high":
                return 10;
            case "normal":
                return 5;
            case "low":
                return 1;
            default:
                return 5;
        }
    }
    
    private void handleValidationError(ValidationException e, long deliveryTag, Channel channel) {
        log.error("Validation error processing message: {}", e.getMessage());
        try {
            channel.basicNack(deliveryTag, false, false); // Don't requeue
        } catch (IOException ex) {
            log.error("Error sending message to DLQ", ex);
        }
    }
    
    private void handleProcessingError(Exception e, long deliveryTag, Channel channel) {
        log.error("Error processing message", e);
        try {
            channel.basicNack(deliveryTag, false, false); // Don't requeue
        } catch (IOException ex) {
            log.error("Error sending message to DLQ", ex);
        }
    }
    
    private MessageContent extractMessageContent(Message message) {
        String filename = message.getMessageProperties().getHeader("CamelFileName");
        Long interfaceId = message.getMessageProperties().getHeader("InterfaceId");
        Long clientId = message.getMessageProperties().getHeader("ClientId");
        
        Interface interfaceEntity = interfaceRepository.findById(interfaceId)
            .orElseThrow(() -> new ValidationException("Interface not found: " + interfaceId));
            
        return new MessageContent(
            message.getBody(),
            filename,
            interfaceId,
            clientId,
            interfaceEntity
        );
    }
    
    private void validateContent(MessageContent content) {
        if (content.getFileContent() == null || content.getFileContent().length == 0) {
            throw new ValidationException("Missing file content");
        }
        if (content.getInterfaceId() == null) {
            throw new ValidationException("Missing InterfaceId");
        }
        if (content.getFilename() == null) {
            throw new ValidationException("Missing filename");
        }
        if (content.getClientId() == null) {
            throw new ValidationException("Missing ClientId");
        }
    }
} 
