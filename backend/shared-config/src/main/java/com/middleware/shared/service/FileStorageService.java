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

@Service
public class FileStorageService {
    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    @Value("${app.file.storage.directory:./storage}")
    private String storageDirectory;

    public void storeFile(ProcessedFile processedFile, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("File is empty or null");
        }

        try {
            if (file.getSize() > 1024 * 1024) { // 1MB threshold
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

    private void storeInFileSystem(ProcessedFile processedFile, MultipartFile file) throws IOException {
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        Path targetLocation = Paths.get(storageDirectory).resolve(fileName);
        
        Files.createDirectories(targetLocation.getParent());
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        
        processedFile.setStorageType("FS");
        processedFile.setFilePath(targetLocation.toString());
        processedFile.setContent("File stored in filesystem: " + fileName);
    }

    private void storeInDatabase(ProcessedFile processedFile, MultipartFile file) throws IOException {
        processedFile.setStorageType("DB");
        processedFile.setContentBytes(file.getBytes());
        processedFile.setContent("File stored in database: " + file.getOriginalFilename());
    }

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