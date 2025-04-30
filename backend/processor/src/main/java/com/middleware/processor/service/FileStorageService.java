package com.middleware.processor.service;

import com.middleware.processor.config.FileStorageConfig;
import com.middleware.processor.exception.ValidationException;
import com.middleware.shared.model.AsnHeader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Service for handling file storage operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final FileStorageConfig config;

    /**
     * Stores an ASN file with compression if enabled
     * @param asnHeader The ASN header
     * @param content The file content
     * @return The path where the file was stored
     */
    public String storeFile(AsnHeader asnHeader, String content) {
        try {
            // Create storage path
            String storagePath = createStoragePath(asnHeader);
            String filename = generateFilename(asnHeader);
            Path fullPath = Paths.get(storagePath, filename);

            // Create directories
            Files.createDirectories(Paths.get(storagePath));

            // Compress content if enabled
            byte[] contentBytes = config.isCompressionEnabled() 
                ? compressContent(content.getBytes())
                : content.getBytes();

            // Write file
            Files.write(fullPath, contentBytes);

            log.info("Stored ASN file at: {}", fullPath);
            return fullPath.toString();

        } catch (IOException e) {
            log.error("Failed to store ASN file: {}", e.getMessage());
            throw new ValidationException("Failed to store ASN file: " + e.getMessage());
        }
    }

    /**
     * Retrieves a stored ASN file
     * @param asnHeader The ASN header
     * @return The file content
     */
    public String retrieveFile(AsnHeader asnHeader) {
        try {
            String storagePath = createStoragePath(asnHeader);
            String filename = generateFilename(asnHeader);
            Path fullPath = Paths.get(storagePath, filename);

            if (!Files.exists(fullPath)) {
                throw new ValidationException("File not found for ASN: " + asnHeader.getAsnNumber());
            }

            byte[] content = Files.readAllBytes(fullPath);
            return config.isCompressionEnabled() 
                ? decompressContent(content)
                : new String(content);

        } catch (IOException e) {
            log.error("Failed to retrieve ASN file: {}", e.getMessage());
            throw new ValidationException("Failed to retrieve ASN file: " + e.getMessage());
        }
    }

    /**
     * Creates the storage path for an ASN file
     */
    private String createStoragePath(AsnHeader asnHeader) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return String.format("%s/%d/%s", 
            config.getBasePath(),
            asnHeader.getClient().getId(),
            datePath);
    }

    /**
     * Generates a filename for an ASN file
     */
    private String generateFilename(AsnHeader asnHeader) {
        return String.format("asn_%s_%d%s", 
            asnHeader.getAsnNumber(),
            System.currentTimeMillis(),
            config.isCompressionEnabled() ? ".gz" : ".xml");
    }

    /**
     * Compresses content using the configured compression level
     */
    private byte[] compressContent(byte[] content) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (DeflaterOutputStream deflaterStream = new DeflaterOutputStream(
                outputStream,
                new Deflater(config.getCompressionLevel()))) {
            deflaterStream.write(content);
        }
        return outputStream.toByteArray();
    }

    /**
     * Decompresses content
     */
    private String decompressContent(byte[] content) throws IOException {
        // Implementation depends on compression algorithm used
        // This is a placeholder - implement actual decompression logic
        return new String(content);
    }

    /**
     * Validates a file before storage
     */
    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("File is empty");
        }

        if (file.getSize() > config.getMaxFileSize()) {
            throw new ValidationException("File size exceeds maximum allowed size");
        }

        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new ValidationException("Invalid filename");
        }

        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        boolean validExtension = false;
        for (String allowed : config.getAllowedExtensions()) {
            if (allowed.equalsIgnoreCase(extension)) {
                validExtension = true;
                break;
            }
        }

        if (!validExtension) {
            throw new ValidationException("File type not allowed");
        }
    }
} 