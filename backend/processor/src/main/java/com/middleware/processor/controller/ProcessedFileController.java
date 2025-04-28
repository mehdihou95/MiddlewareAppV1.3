package com.middleware.processor.controller;

import com.middleware.shared.model.ProcessedFile;
import com.middleware.processor.service.interfaces.ProcessedFileService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/processed-files")
public class ProcessedFileController {

    private final ProcessedFileService processedFileService;

    public ProcessedFileController(ProcessedFileService processedFileService) {
        this.processedFileService = processedFileService;
    }

    @GetMapping
    public ResponseEntity<Page<ProcessedFile>> getProcessedFiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "processedDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String fileNameFilter,
            @RequestParam(required = false) String statusFilter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) Long interfaceId) {
        
        Page<ProcessedFile> processedFiles = processedFileService.getProcessedFiles(
            page, size, sortBy, direction, fileNameFilter, statusFilter, startDate, endDate, clientId, interfaceId);
        return ResponseEntity.ok(processedFiles);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProcessedFile> getProcessedFile(@PathVariable Long id) {
        Optional<ProcessedFile> fileOpt = processedFileService.getProcessedFileById(id);
        return fileOpt.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ProcessedFile> createProcessedFile(@RequestBody ProcessedFile processedFile) {
        return ResponseEntity.ok(processedFileService.createProcessedFile(processedFile));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProcessedFile> updateProcessedFile(@PathVariable Long id, @RequestBody ProcessedFile processedFile) {
        return ResponseEntity.ok(processedFileService.updateProcessedFile(id, processedFile));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProcessedFile(@PathVariable Long id) {
        processedFileService.deleteProcessedFile(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<Page<ProcessedFile>> getProcessedFilesByClient(
            @PathVariable Long clientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "processedDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<ProcessedFile> processedFiles = processedFileService.getProcessedFilesByClient(clientId, pageRequest);
        return ResponseEntity.ok(processedFiles);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<ProcessedFile>> searchProcessedFiles(
            @RequestParam String fileName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "processedDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<ProcessedFile> processedFiles = processedFileService.searchProcessedFiles(fileName, pageRequest);
        return ResponseEntity.ok(processedFiles);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Page<ProcessedFile>> getProcessedFilesByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "processedDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<ProcessedFile> processedFiles = processedFileService.getProcessedFilesByStatus(status, pageRequest);
        return ResponseEntity.ok(processedFiles);
    }

    @GetMapping("/date-range")
    public ResponseEntity<Page<ProcessedFile>> getProcessedFilesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "processedDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<ProcessedFile> processedFiles = processedFileService.getProcessedFilesByDateRange(startDate, endDate, pageRequest);
        return ResponseEntity.ok(processedFiles);
    }

    @GetMapping("/client/{clientId}/status/{status}")
    public ResponseEntity<Page<ProcessedFile>> getProcessedFilesByClientAndStatus(
            @PathVariable Long clientId,
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "processedDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<ProcessedFile> processedFiles = processedFileService.getProcessedFilesByClientAndStatus(clientId, status, pageRequest);
        return ResponseEntity.ok(processedFiles);
    }

    @GetMapping("/client/{clientId}/date-range")
    public ResponseEntity<Page<ProcessedFile>> getProcessedFilesByClientAndDateRange(
            @PathVariable Long clientId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "processedDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<ProcessedFile> processedFiles = processedFileService.getProcessedFilesByClientAndDateRange(clientId, startDate, endDate, pageRequest);
        return ResponseEntity.ok(processedFiles);
    }
} 
