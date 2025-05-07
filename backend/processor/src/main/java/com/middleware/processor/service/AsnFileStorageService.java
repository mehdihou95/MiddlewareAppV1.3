package com.middleware.processor.service;

import com.middleware.shared.service.FileStorageService;
import com.middleware.shared.model.ProcessedFile;
import com.middleware.shared.model.AsnHeader;
import com.middleware.processor.config.FileStorageConfig;
import com.middleware.processor.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Service for handling file storage operations
 */
@Service
public class AsnFileStorageService extends FileStorageService {
    private static final Logger log = LoggerFactory.getLogger(AsnFileStorageService.class);

    @Autowired
    private FileStorageConfig config;

    public void storeAsnFile(AsnHeader asnHeader, MultipartFile file) throws IOException {
        ProcessedFile processedFile = new ProcessedFile();
        processedFile.setFileName(file.getOriginalFilename());
        processedFile.setClient(asnHeader.getClient());
        processedFile.setStatus("NEW");
        processedFile.setAsnHeader(asnHeader);
        
        if (config.isCompressionEnabled()) {
            byte[] compressedContent = compressFile(file.getBytes());
            processedFile.setContentBytes(compressedContent);
            processedFile.setStorageType("DB");
        } else {
            super.storeFile(processedFile, file);
        }
        
        // Set the bidirectional relationship
        asnHeader.setProcessedFile(processedFile);
        log.info("Stored ASN file for header: {}", asnHeader.getId());
    }

    public byte[] retrieveAsnFile(AsnHeader asnHeader) throws IOException {
        ProcessedFile processedFile = asnHeader.getProcessedFile();
        if (processedFile == null) {
            throw new ValidationException("No processed file found for ASN header: " + asnHeader.getId());
        }

        byte[] content = super.retrieveFile(processedFile);
        if ("DB".equals(processedFile.getStorageType()) && config.isCompressionEnabled()) {
            return decompressFile(content);
        }
        return content;
    }

    private byte[] compressFile(byte[] content) throws IOException {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream)) {
            gzipStream.write(content);
            gzipStream.finish();
            return byteStream.toByteArray();
        }
    }

    private byte[] decompressFile(byte[] compressedContent) throws IOException {
        try (ByteArrayInputStream byteStream = new ByteArrayInputStream(compressedContent);
             GZIPInputStream gzipStream = new GZIPInputStream(byteStream);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzipStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, len);
            }
            return outputStream.toByteArray();
        }
    }

    @Override
    protected void validateFile(MultipartFile file) {
        super.validateFile(file);
        
        // Additional ASN-specific validation
        if (!file.getOriginalFilename().toLowerCase().endsWith(".xml")) {
            throw new ValidationException("ASN files must be in XML format");
        }
        
        // Validate file size against ASN-specific limits
        if (file.getSize() > config.getMaxFileSize()) {
            throw new ValidationException("ASN file size exceeds maximum allowed size: " + config.getMaxFileSize());
        }
    }
} 
