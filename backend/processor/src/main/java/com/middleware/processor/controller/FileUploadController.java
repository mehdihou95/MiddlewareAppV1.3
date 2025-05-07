package com.middleware.processor.controller;

import com.middleware.processor.dto.ProcessedFileDTO;
import com.middleware.processor.mapper.ProcessedFileMapper;
import com.middleware.processor.exception.ValidationException;
import com.middleware.shared.model.ProcessedFile;
import com.middleware.shared.model.Interface;
import com.middleware.shared.repository.InterfaceRepository;
import com.middleware.processor.service.interfaces.XmlProcessorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    private static final Logger log = LoggerFactory.getLogger(FileUploadController.class);
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
        "application/xml",
        "text/xml"
    );

    private final XmlProcessorService xmlProcessorService;
    private final ProcessedFileMapper processedFileMapper;
    private final InterfaceRepository interfaceRepository;

    public FileUploadController(XmlProcessorService xmlProcessorService, ProcessedFileMapper processedFileMapper, InterfaceRepository interfaceRepository) {
        this.xmlProcessorService = xmlProcessorService;
        this.processedFileMapper = processedFileMapper;
        this.interfaceRepository = interfaceRepository;
    }

    @PostMapping("/upload/{interfaceId}")
    public ResponseEntity<ProcessedFileDTO> uploadFile(
            @RequestParam("file") MultipartFile file,
            @PathVariable Long interfaceId) {
        log.info("Received file upload request for interface: {}", interfaceId);
        
        // Validate file
        validateFile(file);
        
        // Get interface entity
        Interface interfaceEntity = interfaceRepository.findById(interfaceId)
            .orElseThrow(() -> new ValidationException("Interface not found with id: " + interfaceId));
        
        try {
            // Process file synchronously
            ProcessedFile processedFile = xmlProcessorService.processXmlFile(file, interfaceEntity);
            log.info("Successfully processed file: {}", file.getOriginalFilename());
            return ResponseEntity.ok(processedFileMapper.toDTO(processedFile));
        } catch (Exception e) {
            log.error("Error processing file {}: {}", file.getOriginalFilename(), e.getMessage(), e);
            throw new ValidationException("Failed to process file: " + e.getMessage());
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ValidationException("File size exceeds maximum limit of " + MAX_FILE_SIZE + " bytes");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new ValidationException("Invalid file type. Only XML files are allowed");
        }

        if (file.getOriginalFilename() == null || !file.getOriginalFilename().toLowerCase().endsWith(".xml")) {
            throw new ValidationException("Invalid file name. File must have .xml extension");
        }
    }

    @GetMapping("/processed")
    public ResponseEntity<Page<ProcessedFile>> getProcessedFiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "processedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        PageRequest pageRequest = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(xmlProcessorService.getProcessedFiles(pageRequest));
    }

    @GetMapping("/errors")
    public ResponseEntity<Page<ProcessedFile>> getErrorFiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "processedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        PageRequest pageRequest = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(xmlProcessorService.getErrorFiles(pageRequest));
    }

    @PostMapping("/reprocess/{fileId}")
    public ResponseEntity<Void> reprocessFile(@PathVariable Long fileId) {
        xmlProcessorService.reprocessFile(fileId);
        return ResponseEntity.ok().build();
    }
} 
