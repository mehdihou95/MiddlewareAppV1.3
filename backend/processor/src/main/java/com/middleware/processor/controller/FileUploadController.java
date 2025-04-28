package com.middleware.processor.controller;

import com.middleware.processor.dto.ProcessedFileDTO;
import com.middleware.processor.mapper.ProcessedFileMapper;
import com.middleware.processor.exception.ValidationException;
import com.middleware.shared.model.ProcessedFile;
import com.middleware.shared.model.Interface;
import com.middleware.shared.repository.InterfaceRepository;
import com.middleware.processor.service.interfaces.XmlProcessorService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {

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
        // Get interface entity
        Interface interfaceEntity = interfaceRepository.findById(interfaceId)
            .orElseThrow(() -> new ValidationException("Interface not found with id: " + interfaceId));
        
        // Process file synchronously
        ProcessedFile processedFile = xmlProcessorService.processXmlFile(file, interfaceEntity);
        return ResponseEntity.ok(processedFileMapper.toDTO(processedFile));
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
