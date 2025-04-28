package com.middleware.processor.events;

import com.middleware.shared.model.ProcessedFile;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class DocumentEventPublisher {
    private final ApplicationEventPublisher publisher;

    public DocumentEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publishProcessingStarted(String fileName, String documentType) {
        publisher.publishEvent(new DocumentProcessingStartedEvent(fileName, documentType));
    }

    public void publishProcessingComplete(ProcessedFile processedFile) {
        publisher.publishEvent(new DocumentProcessingCompletedEvent(processedFile));
    }

    public void publishProcessingError(String fileName, Throwable error) {
        publisher.publishEvent(new DocumentProcessingErrorEvent(fileName, error));
    }

    // Event classes
    public record DocumentProcessingStartedEvent(String fileName, String documentType) {}
    
    public record DocumentProcessingCompletedEvent(ProcessedFile processedFile) {}
    
    public record DocumentProcessingErrorEvent(String fileName, Throwable error) {}
} 