package com.middleware.processor.service.impl;

import com.middleware.processor.exception.ValidationException;
import com.middleware.processor.service.interfaces.DocumentProcessingStrategyService;
import com.middleware.processor.service.interfaces.XmlProcessorService;
import com.middleware.processor.service.strategy.BaseDocumentProcessingStrategy;
import com.middleware.processor.service.strategy.DocumentProcessingStrategyFactory;
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
import org.springframework.mock.web.MockMultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Implementation of XmlProcessorService.
 * Handles XML file processing with circuit breaker protection.
 */
@Service
public class XmlProcessorServiceImpl implements XmlProcessorService {

    private static final Logger logger = LoggerFactory.getLogger(XmlProcessorServiceImpl.class);
    private static final int ASYNC_TIMEOUT_MINUTES = 5;

    @Autowired
    private DocumentProcessingStrategyService documentProcessingStrategyService;

    @Autowired
    private CircuitBreakerService circuitBreakerService;

    @Autowired
    private InterfaceRepository interfaceRepository;

    @Autowired
    private ProcessedFileRepository processedFileRepository;

    @Override
    public ProcessedFile processXmlFile(MultipartFile file, Interface interfaceEntity) {
        logger.info("Processing XML file: {} for interface: {}", file.getOriginalFilename(), interfaceEntity.getName());

        Supplier<ProcessedFile> mainOperation = () -> {
            try {
                BaseDocumentProcessingStrategy strategy = documentProcessingStrategyService.getStrategy(interfaceEntity.getType());
                if (strategy == null) {
                    throw new ValidationException("No processing strategy found for interface type: " + interfaceEntity.getType());
                }
                return strategy.processDocument(file, interfaceEntity);
            } catch (Exception e) {
                logger.error("Error processing file {}: {}", file.getOriginalFilename(), e.getMessage(), e);
                throw new RuntimeException("Failed to process file: " + e.getMessage(), e);
            }
        };

        Supplier<ProcessedFile> fallback = () -> {
            logger.error("Circuit breaker is open for file processing: {}", file.getOriginalFilename());
            ProcessedFile errorFile = new ProcessedFile();
            errorFile.setFileName(file.getOriginalFilename());
            errorFile.setStatus("ERROR");
            errorFile.setErrorMessage("Service temporarily unavailable due to high load");
            errorFile.setInterfaceEntity(interfaceEntity);
            return errorFile;
        };

        return circuitBreakerService.executeRepositoryOperation(mainOperation, fallback);
    }

    @Override
    public ProcessedFile processXmlFile(MultipartFile file) {
        logger.info("Processing XML file: {}", file.getOriginalFilename());

        Supplier<ProcessedFile> mainOperation = () -> {
            try {
                BaseDocumentProcessingStrategy strategy = documentProcessingStrategyService.getStrategy(file.getOriginalFilename());
                if (strategy == null) {
                    throw new ValidationException("No processing strategy found for file: " + file.getOriginalFilename());
                }
                // Create a default interface for the file
                Interface defaultInterface = new Interface();
                defaultInterface.setType("DEFAULT");
                return strategy.processDocument(file, defaultInterface);
            } catch (Exception e) {
                logger.error("Error processing file {}: {}", file.getOriginalFilename(), e.getMessage(), e);
                throw new RuntimeException("Failed to process file: " + e.getMessage(), e);
            }
        };

        Supplier<ProcessedFile> fallback = () -> {
            logger.error("Circuit breaker is open for file processing: {}", file.getOriginalFilename());
            ProcessedFile errorFile = new ProcessedFile();
            errorFile.setFileName(file.getOriginalFilename());
            errorFile.setStatus("ERROR");
            errorFile.setErrorMessage("Service temporarily unavailable due to high load");
            return errorFile;
        };

        return circuitBreakerService.executeRepositoryOperation(mainOperation, fallback);
    }

    @Override
    public CompletableFuture<ProcessedFile> processXmlFileAsync(MultipartFile file, Long interfaceId) {
        logger.info("Processing XML file asynchronously: {} for interface: {}", file.getOriginalFilename(), interfaceId);

        return CompletableFuture.supplyAsync(() -> {
            try {
                Interface interfaceEntity = interfaceRepository.findById(interfaceId)
                    .orElseThrow(() -> new ValidationException("Interface not found with ID: " + interfaceId));
                return processXmlFile(file, interfaceEntity);
            } catch (Exception e) {
                logger.error("Async processing failed: {}", e.getMessage(), e);
                ProcessedFile errorFile = new ProcessedFile();
                errorFile.setFileName(file.getOriginalFilename());
                errorFile.setStatus("ERROR");
                errorFile.setErrorMessage("Failed to process file asynchronously: " + e.getMessage());
                return errorFile;
            }
        }).completeOnTimeout(null, ASYNC_TIMEOUT_MINUTES, TimeUnit.MINUTES);
    }

    @Override
    public void reprocessFile(Long fileId) {
        logger.info("Reprocessing file with ID: {}", fileId);

        CircuitBreakerService.VoidOperation mainOperation = () -> {
            try {
                ProcessedFile file = processedFileRepository.findById(fileId)
                    .orElseThrow(() -> new ValidationException("File not found with ID: " + fileId));

                BaseDocumentProcessingStrategy strategy = documentProcessingStrategyService.getStrategy(file.getInterfaceEntity().getType());
                if (strategy == null) {
                    throw new ValidationException("No processing strategy found for interface type: " + file.getInterfaceEntity().getType());
                }

                // Create a MultipartFile from the content
                MultipartFile multipartFile = new MockMultipartFile(
                    file.getFileName(),
                    file.getFileName(),
                    "text/xml",
                    file.getContent().getBytes()
                );

                ProcessedFile reprocessedFile = strategy.processDocument(multipartFile, file.getInterfaceEntity());
                file.setStatus(reprocessedFile.getStatus());
                file.setErrorMessage(reprocessedFile.getErrorMessage());
                processedFileRepository.save(file);
            } catch (Exception e) {
                logger.error("Error reprocessing file {}: {}", fileId, e.getMessage(), e);
                throw new RuntimeException("Failed to reprocess file: " + e.getMessage(), e);
            }
        };

        CircuitBreakerService.VoidOperation fallback = () -> {
            logger.error("Circuit breaker is open for file reprocessing: {}", fileId);
            try {
                ProcessedFile file = processedFileRepository.findById(fileId)
                    .orElseThrow(() -> new ValidationException("File not found with ID: " + fileId));
                file.setStatus("ERROR");
                file.setErrorMessage("Service temporarily unavailable due to high load");
                processedFileRepository.save(file);
            } catch (Exception e) {
                logger.error("Error updating file status: {}", e.getMessage(), e);
            }
        };

        circuitBreakerService.executeVoidRepositoryOperation(mainOperation, fallback);
    }

    @Override
    public boolean validateXmlFile(MultipartFile file, Interface interfaceEntity) {
        logger.info("Validating XML file: {} for interface: {}", file.getOriginalFilename(), interfaceEntity.getName());

        Supplier<Boolean> mainOperation = () -> {
            try {
                BaseDocumentProcessingStrategy strategy = documentProcessingStrategyService.getStrategy(interfaceEntity.getType());
                if (strategy == null) {
                    throw new ValidationException("No processing strategy found for interface type: " + interfaceEntity.getType());
                }
                ProcessedFile result = strategy.processDocument(file, interfaceEntity);
                return "SUCCESS".equals(result.getStatus());
            } catch (Exception e) {
                logger.error("Error validating XML file {}: {}", file.getOriginalFilename(), e.getMessage(), e);
                throw new RuntimeException("Failed to validate XML file: " + e.getMessage(), e);
            }
        };

        Supplier<Boolean> fallback = () -> {
            logger.warn("Circuit breaker fallback: Validation failed for file {}", file.getOriginalFilename());
            return false;
        };

        return circuitBreakerService.executeRepositoryOperation(mainOperation, fallback);
    }

    @Override
    public String transformXmlFile(MultipartFile file, Interface interfaceEntity) {
        logger.info("Transforming XML file: {} for interface: {}", file.getOriginalFilename(), interfaceEntity.getName());

        Supplier<String> mainOperation = () -> {
            try {
                BaseDocumentProcessingStrategy strategy = documentProcessingStrategyService.getStrategy(interfaceEntity.getType());
                if (strategy == null) {
                    throw new ValidationException("No processing strategy found for interface type: " + interfaceEntity.getType());
                }
                ProcessedFile result = strategy.processDocument(file, interfaceEntity);
                return result.getContent();
            } catch (Exception e) {
                logger.error("Error transforming XML file {}: {}", file.getOriginalFilename(), e.getMessage(), e);
                throw new RuntimeException("Failed to transform XML file: " + e.getMessage(), e);
            }
        };

        Supplier<String> fallback = () -> {
            logger.warn("Circuit breaker fallback: Transformation failed for file {}", file.getOriginalFilename());
            throw new RuntimeException("Service temporarily unavailable due to high load");
        };

        return circuitBreakerService.executeRepositoryOperation(mainOperation, fallback);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProcessedFile> getProcessedFiles() {
        logger.info("Retrieving all successfully processed files");
        return circuitBreakerService.executeRepositoryOperation(
            () -> processedFileRepository.findByStatus("SUCCESS"),
            () -> {
                logger.warn("Circuit breaker fallback: Returning empty list for getProcessedFiles");
                return new ArrayList<>();
            }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProcessedFile> getProcessedFiles(Pageable pageable) {
        logger.info("Retrieving processed files with pagination: {}", pageable);
        return circuitBreakerService.executeRepositoryOperation(
            () -> processedFileRepository.findByStatus("SUCCESS", pageable),
            () -> {
                logger.warn("Circuit breaker fallback: Returning empty page for getProcessedFiles");
                return Page.empty(pageable);
            }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProcessedFile> getErrorFiles() {
        logger.info("Retrieving all error files");
        return circuitBreakerService.executeRepositoryOperation(
            () -> processedFileRepository.findByStatus("ERROR"),
            () -> {
                logger.warn("Circuit breaker fallback: Returning empty list for getErrorFiles");
                return new ArrayList<>();
            }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProcessedFile> getErrorFiles(Pageable pageable) {
        logger.info("Retrieving error files with pagination: {}", pageable);
        return circuitBreakerService.executeRepositoryOperation(
            () -> processedFileRepository.findByStatus("ERROR", pageable),
            () -> {
                logger.warn("Circuit breaker fallback: Returning empty page for getErrorFiles");
                return Page.empty(pageable);
            }
        );
    }
}
