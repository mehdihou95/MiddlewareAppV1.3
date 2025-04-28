package com.middleware.processor.service.impl;

import com.middleware.processor.exception.ValidationException;
import com.middleware.processor.service.interfaces.DocumentProcessingStrategyService;
import com.middleware.processor.service.interfaces.XmlProcessorService;
import com.middleware.processor.service.util.XmlProcessor;
import com.middleware.shared.model.Interface;
import com.middleware.shared.model.ProcessedFile;
import com.middleware.shared.repository.InterfaceRepository;
import com.middleware.shared.repository.ProcessedFileRepository;
import com.middleware.shared.service.util.CircuitBreakerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of XmlProcessorService.
 * Handles XML file processing with circuit breaker protection.
 */
@Service
public class XmlProcessorServiceImpl implements XmlProcessorService {

    private static final Logger log = LoggerFactory.getLogger(XmlProcessorServiceImpl.class);
    private static final int ASYNC_TIMEOUT_MINUTES = 5;

    @Autowired
    private DocumentProcessingStrategyService documentProcessingStrategyService;

    @Autowired
    private CircuitBreakerService circuitBreakerService;

    @Autowired
    private XmlProcessor xmlProcessor;

    @Autowired
    private InterfaceRepository interfaceRepository;

    @Autowired
    private ProcessedFileRepository processedFileRepository;

    @Override
    @Transactional
    public ProcessedFile processXmlFile(MultipartFile file, Interface interfaceEntity) throws ValidationException {
        log.debug("Processing XML file {} for interface {}", file.getOriginalFilename(), interfaceEntity.getName());
        
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                try {
                    // Parse XML file
                    Document document = xmlProcessor.parseXmlFile(file);
                    
                    // Extract root element
                    String rootElement = xmlProcessor.extractRootElement(file);
                    log.debug("Extracted root element: {}", rootElement);
                    
                    // Create MultipartFile from document for strategy processing
                    MultipartFile processableFile = xmlProcessor.createMultipartFile(
                        xmlProcessor.serializeDocument(document),
                        file.getOriginalFilename()
                    );
                    
                    // Process document using appropriate strategy
                    return documentProcessingStrategyService.processDocument(processableFile, interfaceEntity);
                } catch (Exception e) {
                    log.error("Error processing XML file {}: {}", file.getOriginalFilename(), e.getMessage(), e);
                    throw new ValidationException("Failed to process XML file: " + e.getMessage(), e);
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Unable to process file {}", file.getOriginalFilename());
                throw new ValidationException("Service unavailable: Circuit breaker open");
            }
        );
    }

    @Override
    @Transactional
    public ProcessedFile processXmlFile(MultipartFile file) throws ValidationException {
        log.debug("Processing XML file {} with auto-detected interface", file.getOriginalFilename());
        
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                try {
                    // Auto-detect interface based on XML content
                    String rootElement = xmlProcessor.extractRootElement(file);
                    Interface detectedInterface = interfaceRepository.findByRootElementAndIsActiveTrue(rootElement)
                        .orElseThrow(() -> new ValidationException("No matching interface found for XML content"));
                    
                    return processXmlFile(file, detectedInterface);
                } catch (Exception e) {
                    log.error("Error processing XML file {}: {}", file.getOriginalFilename(), e.getMessage(), e);
                    throw new ValidationException("Failed to process XML file: " + e.getMessage(), e);
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Creating error processed file for {}", file.getOriginalFilename());
                ProcessedFile errorFile = new ProcessedFile();
                errorFile.setFileName(file.getOriginalFilename());
                errorFile.setStatus("ERROR");
                errorFile.setErrorMessage("Service unavailable: Circuit breaker open");
                errorFile.setProcessedAt(LocalDateTime.now());
                return errorFile;
            }
        );
    }

    @Override
    @Transactional
    public void reprocessFile(Long fileId) throws ValidationException {
        log.debug("Reprocessing file with id: {}", fileId);
        
        circuitBreakerService.executeVoidRepositoryOperation(
            () -> {
                try {
                    ProcessedFile existingFile = processedFileRepository.findById(fileId)
                        .orElseThrow(() -> new ValidationException("Processed file not found with id: " + fileId));
                    
                    // Convert content to MultipartFile
                    MultipartFile contentFile = xmlProcessor.createMultipartFile(
                        existingFile.getContent(), 
                        existingFile.getFileName()
                    );
                    
                    ProcessedFile result = processXmlFile(contentFile, existingFile.getInterfaceEntity());
                    existingFile.setStatus(result.getStatus());
                    existingFile.setErrorMessage(result.getErrorMessage());
                    existingFile.setProcessedAt(LocalDateTime.now());
                    processedFileRepository.save(existingFile);
                } catch (Exception e) {
                    log.error("Error reprocessing file {}: {}", fileId, e.getMessage(), e);
                    throw new ValidationException("Failed to reprocess file: " + e.getMessage(), e);
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Unable to reprocess file {}", fileId);
                throw new ValidationException("Service unavailable: Circuit breaker open");
            }
        );
    }

    @Override
    public CompletableFuture<ProcessedFile> processXmlFileAsync(MultipartFile file, Long interfaceId) {
        log.debug("Processing XML file asynchronously: {} for interface {}", file.getOriginalFilename(), interfaceId);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                Interface interfaceEntity = interfaceRepository.findById(interfaceId)
                    .orElseThrow(() -> new ValidationException("Interface not found with id: " + interfaceId));
                return processXmlFile(file, interfaceEntity);
            } catch (Exception e) {
                log.error("Async processing failed: {}", e.getMessage(), e);
                ProcessedFile errorFile = new ProcessedFile();
                errorFile.setFileName(file.getOriginalFilename());
                errorFile.setStatus("ERROR");
                errorFile.setErrorMessage("Async processing failed: " + e.getMessage());
                errorFile.setProcessedAt(LocalDateTime.now());
                return errorFile;
            }
        }).orTimeout(ASYNC_TIMEOUT_MINUTES, TimeUnit.MINUTES);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProcessedFile> getProcessedFiles() {
        log.debug("Retrieving all successfully processed files");
        return circuitBreakerService.executeRepositoryOperation(
            () -> processedFileRepository.findByStatus("SUCCESS"),
            () -> {
                log.warn("Circuit breaker fallback: Returning empty list for getProcessedFiles");
                return new ArrayList<>();
            }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProcessedFile> getErrorFiles() {
        log.debug("Retrieving all error files");
        return circuitBreakerService.executeRepositoryOperation(
            () -> processedFileRepository.findByStatus("ERROR"),
            () -> {
                log.warn("Circuit breaker fallback: Returning empty list for getErrorFiles");
                return new ArrayList<>();
            }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProcessedFile> getProcessedFiles(Pageable pageable) {
        log.debug("Retrieving processed files with pagination: {}", pageable);
        return circuitBreakerService.executeRepositoryOperation(
            () -> processedFileRepository.findByStatus("SUCCESS", pageable),
            () -> {
                log.warn("Circuit breaker fallback: Returning empty page for getProcessedFiles");
                return Page.empty(pageable);
            }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProcessedFile> getErrorFiles(Pageable pageable) {
        log.debug("Retrieving error files with pagination: {}", pageable);
        return circuitBreakerService.executeRepositoryOperation(
            () -> processedFileRepository.findByStatus("ERROR", pageable),
            () -> {
                log.warn("Circuit breaker fallback: Returning empty page for getErrorFiles");
                return Page.empty(pageable);
            }
        );
    }

    @Override
    public boolean validateXmlFile(MultipartFile file, Interface interfaceEntity) throws ValidationException {
        log.debug("Validating XML file {} for interface {}", file.getOriginalFilename(), interfaceEntity.getName());
        
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                try {
                    // Parse XML file
                    Document document = xmlProcessor.parseXmlFile(file);
                    
                    // Extract root element and verify it matches interface
                    String rootElement = xmlProcessor.extractRootElement(file);
                    if (!rootElement.equals(interfaceEntity.getRootElement())) {
                        throw new ValidationException("Root element does not match interface: expected " + 
                            interfaceEntity.getRootElement() + " but found " + rootElement);
                    }
                    
                    // Create MultipartFile from document for validation
                    MultipartFile processableFile = xmlProcessor.createMultipartFile(
                        xmlProcessor.serializeDocument(document),
                        file.getOriginalFilename()
                    );
                    
                    // Process document to validate
                    ProcessedFile result = documentProcessingStrategyService.processDocument(processableFile, interfaceEntity);
                    return "SUCCESS".equals(result.getStatus());
                } catch (Exception e) {
                    log.error("Error validating XML file {}: {}", file.getOriginalFilename(), e.getMessage(), e);
                    throw new ValidationException("Failed to validate XML file: " + e.getMessage(), e);
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Validation failed for file {}", file.getOriginalFilename());
                return false;
            }
        );
    }

    @Override
    public String transformXmlFile(MultipartFile file, Interface interfaceEntity) throws ValidationException {
        log.debug("Transforming XML file {} for interface {}", file.getOriginalFilename(), interfaceEntity.getName());
        
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                try {
                    // Parse XML file
                    Document document = xmlProcessor.parseXmlFile(file);
                    
                    // Create MultipartFile from document for transformation
                    MultipartFile processableFile = xmlProcessor.createMultipartFile(
                        xmlProcessor.serializeDocument(document),
                        file.getOriginalFilename()
                    );
                    
                    // Process document to transform
                    ProcessedFile result = documentProcessingStrategyService.processDocument(processableFile, interfaceEntity);
                    if (!"SUCCESS".equals(result.getStatus())) {
                        throw new ValidationException("Failed to transform XML file: " + result.getErrorMessage());
                    }
                    
                    return result.getContent();
                } catch (Exception e) {
                    log.error("Error transforming XML file {}: {}", file.getOriginalFilename(), e.getMessage(), e);
                    throw new ValidationException("Failed to transform XML file: " + e.getMessage(), e);
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Transformation failed for file {}", file.getOriginalFilename());
                throw new ValidationException("Service unavailable: Circuit breaker open");
            }
        );
    }
}
