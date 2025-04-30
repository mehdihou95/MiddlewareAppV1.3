package com.middleware.processor.listener;

import com.middleware.processor.service.interfaces.XmlProcessorService;
import com.middleware.shared.repository.InterfaceRepository;
import com.middleware.processor.exception.ValidationException;
import com.middleware.shared.model.ProcessedFile;
import com.middleware.shared.model.Interface;
import com.middleware.processor.model.MessageContent;
import lombok.RequiredArgsConstructor;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.rabbitmq.client.Channel;
import java.io.*;
import java.nio.file.Files;
import com.middleware.processor.service.BatchProcessorService;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@Component
@RequiredArgsConstructor
public class InboundMessageListener {
    
    private final XmlProcessorService xmlProcessorService;
    private final InterfaceRepository interfaceRepository;
    private final BatchProcessorService batchProcessorService;
    
    @Value("${batch.size:100}")
    private int batchSize;
    
    private final Queue<MessageContent> messageQueue = new ConcurrentLinkedQueue<>();
    
    @Transactional
    @RabbitListener(
        queues = "${rabbitmq.queue.inbound.processor}",
        containerFactory = "rabbitListenerContainerFactory"
    )
    public void processMessage(Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        
        try {
            // 1. Extract and validate message content
            MessageContent content = extractMessageContent(message);
            validateContent(content);
            
            // 2. Always add to batch queue for asynchronous processing
            messageQueue.offer(content);
            // Acknowledge message after adding to queue
            channel.basicAck(deliveryTag, false);
            
        } catch (ValidationException e) {
            handleValidationError(e, deliveryTag, channel);
        } catch (Exception e) {
            handleProcessingError(e, deliveryTag, channel);
        }
    }
    
    @Scheduled(fixedRate = 5000) // Process every 5 seconds
    public void processBatch() {
        if (messageQueue.isEmpty()) {
            return;
        }
        
        List<MessageContent> batch = new ArrayList<>();
        while (!messageQueue.isEmpty() && batch.size() < batchSize) {
            batch.add(messageQueue.poll());
        }
        
        if (!batch.isEmpty()) {
            batchProcessorService.processBatch(batch);
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
    
    private MultipartFile createMultipartFile(MessageContent content) {
        return new MultipartFile() {
            @Override
            public String getName() { return content.getFilename(); }
            @Override
            public String getOriginalFilename() { return content.getFilename(); }
            @Override
            public String getContentType() { return "application/xml"; }
            @Override
            public boolean isEmpty() { return content.getFileContent().length == 0; }
            @Override
            public long getSize() { return content.getFileContent().length; }
            @Override
            public byte[] getBytes() { return content.getFileContent(); }
            @Override
            public InputStream getInputStream() { 
                return new ByteArrayInputStream(content.getFileContent()); 
            }
            @Override
            public void transferTo(File dest) throws IOException {
                Files.write(dest.toPath(), content.getFileContent());
            }
        };
    }
    
    private ProcessedFile processFile(MultipartFile file, Long interfaceId) throws Exception {
        Interface interfaceEntity = interfaceRepository.findById(interfaceId)
            .orElseThrow(() -> new ValidationException("Interface not found: " + interfaceId));
        return xmlProcessorService.processXmlFile(file, interfaceEntity);
    }
    
    private void handleValidationError(ValidationException e, long deliveryTag, Channel channel) {
        log.error("Validation error processing message", e);
        try {
            channel.basicNack(deliveryTag, false, false); // Don't requeue
        } catch (IOException ex) {
            log.error("Error rejecting message", ex);
        }
    }
    
    private void handleProcessingError(Exception e, long deliveryTag, Channel channel) {
        log.error("Error processing message", e);
        try {
            channel.basicNack(deliveryTag, false, true); // Requeue for retry
        } catch (IOException ex) {
            log.error("Error rejecting message", ex);
        }
    }
} 