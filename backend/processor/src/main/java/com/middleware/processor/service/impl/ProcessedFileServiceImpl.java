package com.middleware.processor.service.impl;

import com.middleware.processor.exception.ValidationException;
import com.middleware.shared.model.ProcessedFile;
import com.middleware.shared.model.Interface;
import com.middleware.shared.repository.ProcessedFileRepository;
import com.middleware.processor.service.interfaces.ProcessedFileService;
import com.middleware.shared.service.util.CircuitBreakerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import com.middleware.shared.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

/**
 * Implementation of ProcessedFileService with Circuit Breaker pattern.
 * Provides operations for managing processed files.
 */
@Service
public class ProcessedFileServiceImpl implements ProcessedFileService {

    private static final Logger log = LoggerFactory.getLogger(ProcessedFileServiceImpl.class);

    @Autowired
    private ProcessedFileRepository processedFileRepository;
    
    @Autowired
    private CircuitBreakerService circuitBreakerService;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Page<ProcessedFile> getAllProcessedFiles(Pageable pageable) {
        log.debug("Retrieving all processed files with pagination: {}", pageable);
        return circuitBreakerService.<Page<ProcessedFile>>executeRepositoryOperation(
            () -> processedFileRepository.findAll(pageable),
            () -> new PageImpl<>(new ArrayList<>(), pageable, 0)
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Page<ProcessedFile> getProcessedFilesByClient(Long clientId, Pageable pageable) {
        log.debug("Retrieving processed files for client id: {}", clientId);
        return circuitBreakerService.<Page<ProcessedFile>>executeRepositoryOperation(
            () -> processedFileRepository.findBySearchCriteria(null, null, null, null, pageable),
            () -> new PageImpl<>(new ArrayList<>(), pageable, 0)
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Page<ProcessedFile> getProcessedFilesByInterface(Long interfaceId, Pageable pageable) {
        log.debug("Retrieving processed files for interface id: {}", interfaceId);
        return circuitBreakerService.<Page<ProcessedFile>>executeRepositoryOperation(
            () -> processedFileRepository.findByInterfaceEntity_Id(interfaceId, pageable),
            () -> new PageImpl<>(new ArrayList<>(), pageable, 0)
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Page<ProcessedFile> getProcessedFilesByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        log.debug("Retrieving processed files between {} and {}", startDate, endDate);
        return circuitBreakerService.<Page<ProcessedFile>>executeRepositoryOperation(
            () -> processedFileRepository.findByProcessedAtBetween(startDate, endDate, pageable),
            () -> new PageImpl<>(new ArrayList<>(), pageable, 0)
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Page<ProcessedFile> getProcessedFilesByStatus(String status, Pageable pageable) {
        log.debug("Retrieving processed files with status: {}", status);
        return circuitBreakerService.<Page<ProcessedFile>>executeRepositoryOperation(
            () -> processedFileRepository.findByStatus(status, pageable),
            () -> new PageImpl<>(new ArrayList<>(), pageable, 0)
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<ProcessedFile> getProcessedFileById(Long id) {
        log.debug("Retrieving processed file by id: {}", id);
        return circuitBreakerService.<Optional<ProcessedFile>>executeRepositoryOperation(
            () -> processedFileRepository.findById(id),
            () -> Optional.empty()
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ProcessedFile createProcessedFile(ProcessedFile processedFile) {
        log.debug("Creating new processed file: {}", processedFile.getFileName());
        validateProcessedFile(processedFile);
        processedFile.setProcessedAt(LocalDateTime.now());
        return circuitBreakerService.<ProcessedFile>executeRepositoryOperation(
            () -> processedFileRepository.save(processedFile),
            () -> {
                processedFile.setStatus("ERROR");
                processedFile.setErrorMessage("Repository service unavailable: Circuit breaker open");
                return processedFile;
            }
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ProcessedFile updateProcessedFile(Long id, ProcessedFile processedFile) {
        log.debug("Updating processed file with id: {}", id);
        return circuitBreakerService.<ProcessedFile>executeRepositoryOperation(
            () -> {
                ProcessedFile existingFile = processedFileRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("ProcessedFile not found with id: " + id));
                
                validateProcessedFile(processedFile);
                
                existingFile.setFileName(processedFile.getFileName());
                existingFile.setStatus(processedFile.getStatus());
                existingFile.setErrorMessage(processedFile.getErrorMessage());
                existingFile.setInterfaceEntity(processedFile.getInterfaceEntity());
                existingFile.setClient(processedFile.getClient());
                existingFile.setProcessedAt(processedFile.getProcessedAt());
                
                return processedFileRepository.save(existingFile);
            },
            () -> {
                processedFile.setStatus("ERROR");
                processedFile.setErrorMessage("Repository service unavailable: Circuit breaker open");
                return processedFile;
            }
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteProcessedFile(Long id) {
        log.debug("Deleting processed file with id: {}", id);
        circuitBreakerService.executeVoidRepositoryOperation(
            () -> {
                try {
                if (!processedFileRepository.existsById(id)) {
                    throw new ResourceNotFoundException("ProcessedFile not found with id: " + id);
                }
                processedFileRepository.deleteById(id);
                } catch (Exception e) {
                    log.error("Error deleting processed file with id {}: {}", id, e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Unable to delete processed file with id {}", id);
                throw new ValidationException("Repository service unavailable: Circuit breaker open");
            }
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW,readOnly = true)
    public Page<ProcessedFile> getErrorFiles(Pageable pageable) {
        log.debug("Retrieving error files");
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                try {
                    return processedFileRepository.findByStatus("ERROR", pageable);
                } catch (Exception e) {
                    log.error("Error retrieving error files: {}", e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Returning empty page for getErrorFiles");
                return Page.empty(pageable);
            }
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW,readOnly = true)
    public Page<ProcessedFile> getErrorFilesByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        log.debug("Retrieving error files between {} and {}", startDate, endDate);
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                try {
                    return processedFileRepository.findByStatusAndProcessedAtBetween("ERROR", startDate, endDate, pageable);
                } catch (Exception e) {
                    log.error("Error retrieving error files by date range: {}", e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Returning empty page for getErrorFilesByDateRange");
                return Page.empty(pageable);
            }
        );
    }

    private void validateProcessedFile(ProcessedFile processedFile) {
        if (processedFile.getFileName() == null || processedFile.getFileName().trim().isEmpty()) {
            throw new ValidationException("File name is required");
        }
        if (processedFile.getStatus() == null || processedFile.getStatus().trim().isEmpty()) {
            throw new ValidationException("Status is required");
        }
        if (processedFile.getInterfaceEntity() == null || processedFile.getInterfaceEntity().getId() == null) {
            throw new ValidationException("Interface is required");
        }
        if (processedFile.getClient() == null || processedFile.getClient().getId() == null) {
            throw new ValidationException("Client is required");
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW,readOnly = true)
    public Page<ProcessedFile> getProcessedFiles(int page, int size, String sortBy, String direction,
                                               String fileNameFilter, String statusFilter,
                                               LocalDateTime startDate, LocalDateTime endDate,
                                               Long clientId, Long interfaceId) {
        log.debug("Retrieving processed files with filters - page: {}, size: {}, sortBy: {}, direction: {}, " +
                 "fileNameFilter: {}, statusFilter: {}, startDate: {}, endDate: {}, clientId: {}, interfaceId: {}",
                 page, size, sortBy, direction, fileNameFilter, statusFilter, startDate, endDate, clientId, interfaceId);
        
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                try {
                // If both client and interface are specified, use them for filtering
                if (clientId != null && interfaceId != null) {
                    return processedFileRepository.findByClient_IdAndInterfaceEntity_Id(clientId, interfaceId, pageable);
                }
                // If only client is specified
                else if (clientId != null) {
                    return processedFileRepository.findByClient_Id(clientId, pageable);
                }
                // If only interface is specified
                else if (interfaceId != null) {
                    return processedFileRepository.findByInterfaceEntity_Id(interfaceId, pageable);
                }
                // Apply other filters if specified
                else if (statusFilter != null && !statusFilter.isEmpty()) {
                    return processedFileRepository.findByStatus(statusFilter, pageable);
                }
                else if (startDate != null && endDate != null) {
                    return processedFileRepository.findByProcessedAtBetween(startDate, endDate, pageable);
                }
                else if (fileNameFilter != null && !fileNameFilter.isEmpty()) {
                    return processedFileRepository.findByFileNameContainingIgnoreCase(fileNameFilter, pageable);
                }
                // If no filters are specified, return all files
                return processedFileRepository.findAll(pageable);
                } catch (Exception e) {
                    log.error("Error retrieving processed files with filters: {}", e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Returning empty page for getProcessedFiles with filters");
                return Page.empty(pageable);
            }
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW,readOnly = true)
    public Page<ProcessedFile> getProcessedFilesByClient(Long clientId, int page, int size, String sortBy, String sortDirection) {
        log.debug("Retrieving processed files for client id {} with pagination and sorting", clientId);
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        return getProcessedFilesByClient(clientId, pageable);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW,readOnly = true)
    public Page<ProcessedFile> getProcessedFilesByClientAndStatus(Long clientId, String status, 
                                                                int page, int size, String sortBy, String sortDirection) {
        log.debug("Retrieving processed files for client id {} with status {} and pagination", clientId, status);
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                try {
                    return processedFileRepository.findByClientIdAndStatus(clientId, status, pageable);
                } catch (Exception e) {
                    log.error("Error retrieving processed files for client id {} with status {}: {}", 
                            clientId, status, e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Returning empty page for getProcessedFilesByClientAndStatus");
                return Page.empty(pageable);
            }
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW,readOnly = true)
    public Page<ProcessedFile> getProcessedFilesByClientAndDateRange(Long clientId, LocalDateTime startDate, LocalDateTime endDate,
                                                                   int page, int size, String sortBy, String sortDirection) {
        log.debug("Retrieving processed files for client id {} between {} and {} with pagination", 
                 clientId, startDate, endDate);
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                try {
                    return processedFileRepository.findByClientIdAndProcessedAtBetween(clientId, startDate, endDate, pageable);
                } catch (Exception e) {
                    log.error("Error retrieving processed files for client id {} by date range: {}", 
                            clientId, e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Returning empty page for getProcessedFilesByClientAndDateRange");
                return Page.empty(pageable);
            }
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW,readOnly = true)
    public List<ProcessedFile> getProcessedFilesByStatus(String status) {
        log.debug("Retrieving processed files with status: {}", status);
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                try {
                    return processedFileRepository.findByStatus(status);
                } catch (Exception e) {
                    log.error("Error retrieving processed files by status {}: {}", status, e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Returning empty list for getProcessedFilesByStatus");
                return new ArrayList<>();
            }
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW,readOnly = true)
    public Page<ProcessedFile> getProcessedFilesByClient(Long clientId, PageRequest pageRequest) {
        log.debug("Retrieving processed files for client id {} with page request", clientId);
        return getProcessedFilesByClient(clientId, pageRequest);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW,readOnly = true)
    public Page<ProcessedFile> searchProcessedFiles(String fileName, PageRequest pageRequest) {
        log.debug("Searching processed files by filename: {}", fileName);
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                try {
                    return processedFileRepository.findByFileNameContainingIgnoreCase(fileName, pageRequest);
                } catch (Exception e) {
                    log.error("Error searching processed files by filename {}: {}", fileName, e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Returning empty page for searchProcessedFiles");
                return Page.empty(pageRequest);
            }
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW,readOnly = true)
    public Page<ProcessedFile> getProcessedFilesByStatus(String status, PageRequest pageRequest) {
        log.debug("Retrieving processed files with status {} and page request", status);
        return getProcessedFilesByStatus(status, pageRequest);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW,readOnly = true)
    public Page<ProcessedFile> getProcessedFilesByDateRange(LocalDateTime startDate, LocalDateTime endDate, PageRequest pageRequest) {
        log.debug("Retrieving processed files between {} and {} with page request", startDate, endDate);
        return getProcessedFilesByDateRange(startDate, endDate, pageRequest);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW,readOnly = true)
    public Page<ProcessedFile> getProcessedFilesByClientAndStatus(Long clientId, String status, PageRequest pageRequest) {
        log.debug("Retrieving processed files for client id {} with status {} and page request", clientId, status);
        return getProcessedFilesByClientAndStatus(clientId, status, pageRequest);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW,readOnly = true)
    public Page<ProcessedFile> getProcessedFilesByClientAndDateRange(Long clientId, LocalDateTime startDate, LocalDateTime endDate, PageRequest pageRequest) {
        log.debug("Retrieving processed files for client id {} between {} and {} with page request", 
                 clientId, startDate, endDate);
        return getProcessedFilesByClientAndDateRange(clientId, startDate, endDate, pageRequest);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW,readOnly = true)
    public Optional<ProcessedFile> findByFileNameAndInterfaceId(String fileName, Long interfaceId) {
        log.debug("Finding processed file by filename {} and interface id {}", fileName, interfaceId);
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                try {
                    return processedFileRepository.findByFileNameAndInterfaceEntity_Id(fileName, interfaceId);
                } catch (Exception e) {
                    log.error("Error finding processed file by filename {} and interface id {}: {}", 
                            fileName, interfaceId, e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Returning empty optional for findByFileNameAndInterfaceId");
                return Optional.empty();
            }
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW,readOnly = true)
    public Optional<ProcessedFile> findMostRecentByFileNameAndInterfaceId(String fileName, Long interfaceId) {
        log.debug("Finding most recent processed file by filename {} and interface id {}", fileName, interfaceId);
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                try {
                PageRequest pageRequest = PageRequest.of(0, 1);
                List<ProcessedFile> results = processedFileRepository.findMostRecentByFileNameAndInterfaceId(fileName, interfaceId, pageRequest);
                return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
                } catch (Exception e) {
                    log.error("Error finding most recent processed file by filename {} and interface id {}: {}", 
                            fileName, interfaceId, e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Returning empty optional for findMostRecentByFileNameAndInterfaceId");
                return Optional.empty();
            }
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW,readOnly = true)
    public Page<ProcessedFile> findProcessedFiles(
            Long clientId, 
            Long interfaceId, 
            String status, 
            String fileName, 
            LocalDateTime startDate, 
            LocalDateTime endDate,
            Pageable pageable) {
        log.debug("Finding processed files with filters - clientId: {}, interfaceId: {}, status: {}, " +
                 "fileName: {}, startDate: {}, endDate: {}", 
                 clientId, interfaceId, status, fileName, startDate, endDate);
        
        return circuitBreakerService.<Page<ProcessedFile>>executeRepositoryOperation(
            () -> {
                try {
                    // If both client and interface are specified, use them for filtering
                    if (clientId != null && interfaceId != null) {
                        return processedFileRepository.findByClient_IdAndInterfaceEntity_Id(clientId, interfaceId, pageable);
                    }
                    // If only client is specified
                    else if (clientId != null) {
                        return processedFileRepository.findByClient_Id(clientId, pageable);
                    }
                    // If only interface is specified
                    else if (interfaceId != null) {
                        return processedFileRepository.findByInterfaceEntity_Id(interfaceId, pageable);
                    }
                    // Apply other filters if specified
                    else if (status != null && !status.isEmpty()) {
                        return processedFileRepository.findByStatus(status, pageable);
                    }
                    else if (startDate != null && endDate != null) {
                        return processedFileRepository.findByProcessedAtBetween(startDate, endDate, pageable);
                    }
                    else if (fileName != null && !fileName.isEmpty()) {
                        return processedFileRepository.findByFileNameContainingIgnoreCase(fileName, pageable);
                    }
                    // If no filters are specified, return all files
                    return processedFileRepository.findAll(pageable);
                } catch (Exception e) {
                    log.error("Error finding processed files with filters: {}", e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Returning empty page for findProcessedFiles");
                return new PageImpl<>(new ArrayList<>(), pageable, 0);
            }
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ProcessedFile findOrCreateProcessedFile(String fileName, Interface interfaceEntity, ProcessedFile defaultFile) {
        log.debug("Finding or creating processed file with filename {} for interface {}", 
                 fileName, interfaceEntity.getName());
        
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                try {
                    // Try to find existing file
                    Optional<ProcessedFile> existingFile = processedFileRepository.findByFileNameAndInterfaceEntity_Id(fileName, interfaceEntity.getId());
                    
                    if (existingFile.isPresent()) {
                        return existingFile.get();
                    }
                    
                    // If not found, create new file with default values
                    defaultFile.setFileName(fileName);
                    defaultFile.setInterfaceEntity(interfaceEntity);
                    defaultFile.setClient(interfaceEntity.getClient());
                    defaultFile.setProcessedAt(LocalDateTime.now());
                    
                    return processedFileRepository.save(defaultFile);
                } catch (Exception e) {
                    log.error("Error finding or creating processed file: {}", e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Setting default file {} to error status", defaultFile.getFileName());
                defaultFile.setStatus("ERROR");
                defaultFile.setErrorMessage("Repository service unavailable: Circuit breaker open");
                return defaultFile;
            }
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ProcessedFile atomicUpdateProcessedFile(Long id, String status, String errorMessage, String content) {
        log.debug("Atomically updating processed file with id {} - status: {}, errorMessage: {}", 
                 id, status, errorMessage);
        
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                try {
                    ProcessedFile file = processedFileRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("ProcessedFile not found with id: " + id));
                    
                    // Update fields if provided
                    if (status != null) {
                        file.setStatus(status);
                    }
                    if (errorMessage != null) {
                        file.setErrorMessage(errorMessage);
                    }
                    if (content != null) {
                        file.setContent(content);
                    }
                    
                    return processedFileRepository.save(file);
                } catch (Exception e) {
                    log.error("Error atomically updating processed file with id {}: {}", id, e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Fallback: Could not update processed file with id {}", id);
				ProcessedFile errorFile = new ProcessedFile();
                errorFile.setId(id);
                errorFile.setStatus("ERROR");
                errorFile.setErrorMessage("Repository service unavailable: Circuit breaker open");
                return errorFile;
            }
        );
    }
}
