package com.middleware.processor.listener;

import com.middleware.processor.service.interfaces.XmlProcessorService;
import com.middleware.shared.repository.InterfaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.rabbitmq.client.Channel;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import jakarta.persistence.EntityManager;
import java.io.*;
import java.nio.file.Files;

@Slf4j
@Component
@RequiredArgsConstructor
public class InboundMessageListener {
    
    private final XmlProcessorService xmlProcessorService;
    private final InterfaceRepository interfaceRepository;
    private final RabbitTemplate rabbitTemplate;
    private final EntityManager entityManager;

    @Transactional
    @RabbitListener(queues = "inbound.processor", ackMode = "MANUAL")
    public void processMessage(Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            // Get the file content and headers
            byte[] fileContent = message.getBody();
            String filename = message.getMessageProperties().getHeader("CamelFileName");
            Long interfaceId = message.getMessageProperties().getHeader("InterfaceId");
            Long clientId = message.getMessageProperties().getHeader("ClientId");

            // Add debug logging for headers
            log.debug("Message headers: {}", message.getMessageProperties().getHeaders());

            // Validate required headers
            if (fileContent == null) {
                throw new IllegalArgumentException("Missing file content");
            }
            if (interfaceId == null) {
                throw new IllegalArgumentException("Missing required header: InterfaceId");
            }
            if (filename == null) {
                throw new IllegalArgumentException("Missing required header: CamelFileName");
            }
            if (clientId == null) {
                throw new IllegalArgumentException("Missing required header: ClientId");
            }

            log.info("Processing XML file: filename={}, clientId={}, interfaceId={}", 
                filename, clientId, interfaceId);

            // Create a MultipartFile from the content
            MultipartFile file = new MultipartFile() {
                @Override
                public String getName() { return filename; }
                @Override
                public String getOriginalFilename() { return filename; }
                @Override
                public String getContentType() { return "application/xml"; }
                @Override
                public boolean isEmpty() { return fileContent == null || fileContent.length == 0; }
                @Override
                public long getSize() { return fileContent.length; }
                @Override
                public byte[] getBytes() { return fileContent; }
                @Override
                public InputStream getInputStream() { return new ByteArrayInputStream(fileContent); }
                @Override
                public void transferTo(File dest) throws IOException, IllegalStateException {
                    Files.write(dest.toPath(), fileContent);
                }
            };

            // Process the file asynchronously
            xmlProcessorService.processXmlFileAsync(file, interfaceId)
                .thenAccept(result -> {
                    try {
                        log.info("File processing completed: filename={}, status={}", filename, result.getStatus());
                        // Acknowledge message only after successful processing
                        channel.basicAck(deliveryTag, false);
                    } catch (IOException e) {
                        log.error("Error acknowledging message: ", e);
                    }
                })
                .exceptionally(e -> {
                    log.error("Error processing file: ", e);
                    try {
                        // Negative acknowledge and requeue the message
                        channel.basicNack(deliveryTag, false, true);
                    } catch (IOException ex) {
                        log.error("Error nacking message: ", ex);
                    }
                    return null;
                });

        } catch (Exception e) {
            log.error("Error processing message: ", e);
            try {
                // Negative acknowledge and requeue the message
                channel.basicNack(deliveryTag, false, true);
            } catch (IOException ex) {
                log.error("Error nacking message: ", ex);
            }
        }
    }
} 