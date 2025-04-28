package com.middleware.processor.service.interfaces;

import com.middleware.shared.model.Interface;
import com.middleware.shared.model.ProcessedFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for processing XML files.
 * Provides methods for validating, transforming, and processing XML files.
 */
public interface XmlProcessorService {
    /**
     * Process an XML file for a given interface.
     *
     * @param file The XML file to process
     * @param interfaceEntity The interface to process the file for
     * @return The processed file record
     */
    ProcessedFile processXmlFile(MultipartFile file, Interface interfaceEntity);

    /**
     * Process an XML file.
     *
     * @param file The XML file to process
     * @return The processed file record
     */
    ProcessedFile processXmlFile(MultipartFile file);

    /**
     * Process an XML file asynchronously.
     *
     * @param file The XML file to process
     * @param interfaceId The ID of the interface
     * @return A CompletableFuture containing the processed file record
     */
    CompletableFuture<ProcessedFile> processXmlFileAsync(MultipartFile file, Long interfaceId);

    /**
     * Reprocess a file.
     *
     * @param fileId The ID of the file to reprocess
     */
    void reprocessFile(Long fileId);

    /**
     * Validate an XML file against its interface's XSD schema.
     *
     * @param file The XML file to validate
     * @param interfaceEntity The interface containing the XSD schema
     * @return true if the file is valid, false otherwise
     */
    boolean validateXmlFile(MultipartFile file, Interface interfaceEntity);

    /**
     * Transform an XML file according to the interface's mapping rules.
     *
     * @param file The XML file to transform
     * @param interfaceEntity The interface containing the mapping rules
     * @return The transformed XML as a string
     */
    String transformXmlFile(MultipartFile file, Interface interfaceEntity);

    /**
     * Get all processed files.
     *
     * @return List of processed files
     */
    List<ProcessedFile> getProcessedFiles();

    /**
     * Get all processed files with pagination.
     *
     * @param pageable The pageable object to paginate the results
     * @return Page of processed files
     */
    Page<ProcessedFile> getProcessedFiles(Pageable pageable);

    /**
     * Get all error files.
     *
     * @return List of error files
     */
    List<ProcessedFile> getErrorFiles();

    /**
     * Get all error files with pagination.
     *
     * @param pageable The pageable object to paginate the results
     * @return Page of error files
     */
    Page<ProcessedFile> getErrorFiles(Pageable pageable);
} 
