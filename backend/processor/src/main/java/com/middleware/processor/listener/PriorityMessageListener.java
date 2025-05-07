package com.middleware.processor.listener;

import com.middleware.processor.service.BatchProcessorService;
import com.middleware.processor.model.MessageContent;
import com.middleware.processor.exception.ValidationException;
import com.middleware.processor.metrics.ProcessingMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.rabbitmq.client.Channel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PriorityMessageListener {
    
    private final BatchProcessorService batchProcessorService;
    private final ProcessingMetrics processingMetrics;
    private final RabbitTemplate rabbitTemplate;
    
    @Transactional
    @RabbitListener(
        queues = "${rabbitmq.queue.inbound.processor.high}",
        containerFactory = "rabbitListenerContainerFactory"
    )
    public void processHighPriorityMessage(Message message, Channel channel) {
        processMessage(message, channel, "high");
    }
    
    @Transactional
    @RabbitListener(
        queues = "${rabbitmq.queue.inbound.processor.normal}",
        containerFactory = "rabbitListenerContainerFactory"
    )
    public void processNormalPriorityMessage(Message message, Channel channel) {
        processMessage(message, channel, "normal");
    }
    
    @Transactional
    @RabbitListener(
        queues = "${rabbitmq.queue.inbound.processor.low}",
        containerFactory = "rabbitListenerContainerFactory"
    )
    public void processLowPriorityMessage(Message message, Channel channel) {
        processMessage(message, channel, "low");
    }
    
    private void processMessage(Message message, Channel channel, String priority) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        
        try {
            // 1. Extract and validate message content
            MessageContent content = extractMessageContent(message);
            validateContent(content);
            
            // 2. Process message using batch processor
            List<MessageContent> singleMessageBatch = new ArrayList<>();
            singleMessageBatch.add(content);
            batchProcessorService.processBatch(singleMessageBatch);
            
            // 3. Acknowledge message after successful processing
            channel.basicAck(deliveryTag, false);
            
            // 4. Record metrics
            processingMetrics.getCounters().processedMessages(priority).increment();
            processingMetrics.getCounters().processingSuccess("priority").increment();
            
        } catch (ValidationException e) {
            handleValidationError(e, deliveryTag, channel, priority);
        } catch (Exception e) {
            handleProcessingError(e, deliveryTag, channel, priority);
        }
    }
    
    private void handleValidationError(ValidationException e, long deliveryTag, Channel channel, String priority) {
        log.error("Validation error processing {} priority message: {}", priority, e.getMessage());
        try {
            // Send to DLQ
            MessageProperties properties = new MessageProperties();
            properties.setHeader("x-death", Collections.singletonList(
                Collections.singletonMap("reason", "validation_error")
            ));
            Message dlqMessage = new Message(e.getMessage().getBytes(), properties);
            rabbitTemplate.send("middleware.dlx", "inbound.processor.dlq", dlqMessage);
            
            // Acknowledge original message
            channel.basicAck(deliveryTag, false);
            
            // Record metrics
            processingMetrics.getCounters().processingErrors("validation").increment();
        } catch (Exception ex) {
            log.error("Error handling validation error", ex);
        }
    }
    
    private void handleProcessingError(Exception e, long deliveryTag, Channel channel, String priority) {
        log.error("Error processing {} priority message", priority, e);
        try {
            // Send to DLQ
            MessageProperties properties = new MessageProperties();
            properties.setHeader("x-death", Collections.singletonList(
                Collections.singletonMap("reason", "processing_error")
            ));
            Message dlqMessage = new Message(e.getMessage().getBytes(), properties);
            rabbitTemplate.send("middleware.dlx", "inbound.processor.dlq", dlqMessage);
            
            // Acknowledge original message
            channel.basicAck(deliveryTag, false);
            
            // Record metrics
            processingMetrics.getCounters().processingErrors("processing").increment();
        } catch (Exception ex) {
            log.error("Error handling processing error", ex);
        }
    }
    
    private MessageContent extractMessageContent(Message message) {
        String filename = message.getMessageProperties().getHeader("CamelFileName");
        Long interfaceId = message.getMessageProperties().getHeader("InterfaceId");
        Long clientId = message.getMessageProperties().getHeader("ClientId");
        
        return new MessageContent(
            message.getBody(),
            filename,
            interfaceId,
            clientId,
            null // Interface entity will be loaded in the batch processor
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