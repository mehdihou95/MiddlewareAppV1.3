package com.middleware.shared.service;

import com.middleware.shared.model.ProcessedFile;
import com.middleware.shared.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

/**
 * Abstract base service for file storage operations.
 * Provides common functionality for storing and retrieving files.
 */
@Service
public abstract class FileStorageService {
    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    @Value("${app.file.storage.directory:./storage}")
    protected String storageDirectory;

    @Value("${app.file.storage.max-size:1048576}")
    protected long maxFileSize;

    @Value("${app.file.storage.allowed-extensions:xml,json,csv}")
    protected List<String> allowedExtensions;

    /**
     * Stores a file based on its size and configuration.
     * @param processedFile The processed file metadata
     * @param file The file to store
     * @throws IOException if storage fails
     */
    public void storeFile(ProcessedFile processedFile, MultipartFile file) throws IOException {
        validateFile(file);
        
        try {
            if (shouldStoreInFileSystem(file)) {
                storeInFileSystem(processedFile, file);
            } else {
                storeInDatabase(processedFile, file);
            }
            log.info("Successfully stored file: {} with storage type: {}", 
                    processedFile.getFileName(), processedFile.getStorageType());
        } catch (IOException e) {
            log.error("Error storing file: {}", e.getMessage(), e);
            throw new ValidationException("Failed to store file: " + e.getMessage());
        }
    }

    /**
     * Determines if a file should be stored in the filesystem.
     * Can be overridden by subclasses to implement custom storage strategies.
     * @param file The file to check
     * @return true if the file should be stored in filesystem
     */
    protected boolean shouldStoreInFileSystem(MultipartFile file) {
        return file.getSize() > maxFileSize;
    }

    /**
     * Validates the file before storage.
     * Can be overridden by subclasses to add custom validation rules.
     * @param file The file to validate
     * @throws ValidationException if validation fails
     */
    protected void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("File is empty or null");
        }

        if (file.getSize() > maxFileSize) {
            throw new ValidationException("File size exceeds maximum allowed size");
        }

        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new ValidationException("Invalid filename");
        }

        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        if (!allowedExtensions.contains(extension)) {
            throw new ValidationException("File type not allowed. Allowed types: " + allowedExtensions);
        }
    }

    /**
     * Stores a file in the filesystem.
     * @param processedFile The processed file metadata
     * @param file The file to store
     * @throws IOException if storage fails
     */
    protected void storeInFileSystem(ProcessedFile processedFile, MultipartFile file) throws IOException {
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        Path targetLocation = Paths.get(storageDirectory).resolve(fileName);
        
        Files.createDirectories(targetLocation.getParent());
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        
        processedFile.setStorageType("FS");
        processedFile.setFilePath(targetLocation.toString());
        processedFile.setContent("File stored in filesystem: " + fileName);
    }

    /**
     * Stores a file in the database.
     * @param processedFile The processed file metadata
     * @param file The file to store
     * @throws IOException if storage fails
     */
    protected void storeInDatabase(ProcessedFile processedFile, MultipartFile file) throws IOException {
        processedFile.setStorageType("DB");
        processedFile.setContentBytes(file.getBytes());
        processedFile.setContent("File stored in database: " + file.getOriginalFilename());
    }

    /**
     * Retrieves a file based on its storage type.
     * @param processedFile The processed file metadata
     * @return The file content as bytes
     * @throws IOException if retrieval fails
     */
    public byte[] retrieveFile(ProcessedFile processedFile) throws IOException {
        if (processedFile == null) {
            throw new ValidationException("ProcessedFile is null");
        }

        try {
            if ("FS".equals(processedFile.getStorageType())) {
                return Files.readAllBytes(Paths.get(processedFile.getFilePath()));
            } else {
                return processedFile.getContentBytes();
            }
        } catch (IOException e) {
            log.error("Error retrieving file: {}", e.getMessage(), e);
            throw new ValidationException("Failed to retrieve file: " + e.getMessage());
        }
    }
} 
