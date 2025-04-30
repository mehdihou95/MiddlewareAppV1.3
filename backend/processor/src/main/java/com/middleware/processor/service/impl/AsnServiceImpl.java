package com.middleware.processor.service.impl;

import com.middleware.processor.exception.ValidationException;
import com.middleware.shared.model.AsnHeader;
import com.middleware.shared.model.AsnLine;
import com.middleware.shared.repository.AsnHeaderRepository;
import com.middleware.shared.repository.AsnLineRepository;
import com.middleware.processor.service.interfaces.AsnService;
import com.middleware.shared.service.util.CircuitBreakerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import com.middleware.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.time.Instant;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Implementation of AsnService with Circuit Breaker pattern.
 * Provides operations for managing ASN documents.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsnServiceImpl implements AsnService {

    private final AsnHeaderRepository asnHeaderRepository;
    private final AsnLineRepository asnLineRepository;
    private final CircuitBreakerService circuitBreakerService;

    @Value("${asn.batch.min-size:10}")
    private int minBatchSize;

    @Value("${asn.batch.max-size:1000}")
    private int maxBatchSize;

    @Value("${asn.batch.initial-size:100}")
    private int initialBatchSize;

    @Value("${asn.batch.adjustment-step:10}")
    private int batchAdjustmentStep;

    @Value("${asn.batch.load-threshold:0.8}")
    private double loadThreshold;

    private final AtomicInteger currentBatchSize = new AtomicInteger(100);
    private final AtomicLong lastBatchTime = new AtomicLong(0);
    private final AtomicInteger batchCount = new AtomicInteger(0);
    private final AtomicLong totalProcessingTime = new AtomicLong(0);

    @Override
    @Transactional(readOnly = true)
    public List<AsnHeader> getAllAsnHeaders() {
        return circuitBreakerService.executeRepositoryOperation(
            () -> asnHeaderRepository.findAll(),
            () -> new ArrayList<AsnHeader>() // Fallback: return empty list
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AsnHeader> getAllAsnHeaders(Pageable pageable) {
        return circuitBreakerService.executeRepositoryOperation(
            () -> asnHeaderRepository.findAll(pageable),
            () -> Page.empty(pageable) // Fallback: return empty page
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AsnHeader> getAsnHeaderById(Long id) {
        return circuitBreakerService.executeRepositoryOperation(
            () -> asnHeaderRepository.findById(id),
            Optional::empty // Fallback: return empty optional
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<AsnHeader> getAsnHeadersByClient(Long clientId) {
        return circuitBreakerService.executeRepositoryOperation(
            () -> asnHeaderRepository.findByClient_Id(clientId),
            () -> new ArrayList<AsnHeader>() // Fallback: return empty list
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AsnHeader> getAsnHeadersByClient(Long clientId, Pageable pageable) {
        return circuitBreakerService.executeRepositoryOperation(
            () -> asnHeaderRepository.findByClient_Id(clientId, pageable),
            () -> Page.empty(pageable) // Fallback: return empty page
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AsnHeader> getAsnHeadersByClient_Id(Long clientId, Pageable pageable) {
        return circuitBreakerService.<Page<AsnHeader>>executeRepositoryOperation(
            () -> asnHeaderRepository.findByClient_Id(clientId, pageable),
            () -> Page.empty(pageable)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AsnHeader> findByAsnNumberAndClient_Id(String asnNumber, Long clientId) {
        return circuitBreakerService.executeRepositoryOperation(
            () -> asnHeaderRepository.findByAsnNumberAndClient_IdWithClientAndLines(asnNumber, clientId),
            Optional::empty
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AsnHeader> findByClient_IdAndReceiptDttmBetween(Long clientId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return circuitBreakerService.executeRepositoryOperation(
            () -> asnHeaderRepository.findByClient_IdAndReceiptDttmBetween(clientId, startDate, endDate, pageable),
            () -> Page.empty()
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public AsnHeader createAsnHeader(AsnHeader header) {
        validateAsnHeader(header);
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                // Save the header with entity graph
                AsnHeader savedHeader = asnHeaderRepository.save(header);
                
                // Verify ID was generated
                if (savedHeader.getId() == null) {
                    throw new ValidationException("Failed to generate ID for ASN Header");
                }
                
                // Return the saved header with entity graph
                return savedHeader;
            },
            () -> {
                header.setStatus("ERROR - Circuit breaker open");
                return header;
            }
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public AsnHeader updateAsnHeader(Long id, AsnHeader headerDetails) {
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                AsnHeader existingHeader = asnHeaderRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("AsnHeader not found with id: " + id));
                
                validateAsnHeader(headerDetails);
                
                // Update fields
                existingHeader.setAsnNumber(headerDetails.getAsnNumber());
                existingHeader.setStatus(headerDetails.getStatus());
                existingHeader.setClient(headerDetails.getClient());
                existingHeader.setReceiptDttm(headerDetails.getReceiptDttm());
                
                return asnHeaderRepository.save(existingHeader);
            },
            () -> {
                // Fallback: return the original header with error status
                headerDetails.setStatus("ERROR");
                return headerDetails;
            }
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void deleteAsnHeader(Long id) {
        circuitBreakerService.executeVoidRepositoryOperation(
            () -> {
                if (!asnHeaderRepository.existsById(id)) {
                    throw new ResourceNotFoundException("AsnHeader not found with id: " + id);
                }
                asnHeaderRepository.deleteById(id);
            },
            () -> {
                // Fallback: log error but don't throw exception
                throw new ValidationException("Repository service unavailable: Circuit breaker open");
            }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<AsnLine> getAsnLinesByHeader(Long headerId) {
        return circuitBreakerService.<List<AsnLine>>executeRepositoryOperation(
            () -> asnLineRepository.findByHeader_Id(headerId),
            () -> new ArrayList<AsnLine>()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AsnLine> getAsnLinesByHeader(Long headerId, Pageable pageable) {
        return circuitBreakerService.executeRepositoryOperation(
            () -> asnLineRepository.findByHeader_Id(headerId, pageable),
            () -> Page.empty(pageable)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AsnLine> getAsnLinesByHeader_Id(Long headerId, Pageable pageable) {
        return getAsnLinesByHeader(headerId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AsnLine> getAsnLineById(Long id) {
        return circuitBreakerService.executeRepositoryOperation(
            () -> asnLineRepository.findById(id),
            Optional::empty // Fallback: return empty optional
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AsnLine> getAllAsnLines(Pageable pageable) {
        return circuitBreakerService.executeRepositoryOperation(
            () -> asnLineRepository.findAll(pageable),
            () -> Page.empty(pageable) // Fallback: return empty page
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AsnLine> getAsnLinesByClient_Id(Long clientId, Pageable pageable) {
        return circuitBreakerService.executeRepositoryOperation(
            () -> asnLineRepository.findByClient_Id(clientId, pageable),
            () -> Page.empty(pageable) // Fallback: return empty page
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AsnLine> findByClient_IdAndHeader_Id(Long clientId, Long headerId, Pageable pageable) {
        return circuitBreakerService.executeRepositoryOperation(
            () -> asnLineRepository.findByClient_IdAndHeader_Id(clientId, headerId, pageable),
            () -> Page.empty(pageable) // Fallback: return empty page
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AsnLine> findByClient_IdAndItemNumber(Long clientId, String itemNumber, Pageable pageable) {
        return circuitBreakerService.executeRepositoryOperation(
            () -> asnLineRepository.findByClient_IdAndItemNumber(clientId, itemNumber, pageable),
            () -> Page.empty(pageable) // Fallback: return empty page
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AsnLine> findByClient_IdAndLotNumber(Long clientId, String lotNumber, Pageable pageable) {
        return circuitBreakerService.executeRepositoryOperation(
            () -> asnLineRepository.findByClient_IdAndLotNumber(clientId, lotNumber, pageable),
            () -> Page.empty(pageable) // Fallback: return empty page
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AsnLine> findByClient_IdAndStatus(Long clientId, String status, Pageable pageable) {
        return circuitBreakerService.executeRepositoryOperation(
            () -> asnLineRepository.findByClient_IdAndStatus(clientId, status, pageable),
            () -> Page.empty(pageable) // Fallback: return empty page
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AsnLine> findByClient_IdAndQuantityGreaterThan(Long clientId, Integer quantity, Pageable pageable) {
        return circuitBreakerService.executeRepositoryOperation(
            () -> asnLineRepository.findByClient_IdAndQuantityGreaterThan(clientId, quantity, pageable),
            () -> Page.empty(pageable) // Fallback: return empty page
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public AsnLine createAsnLine(AsnLine line) {
        validateAsnLine(line);
        return circuitBreakerService.executeRepositoryOperation(
            () -> asnLineRepository.save(line),
            () -> {
                // Fallback: return the original line with error status
                line.setStatus("ERROR");
                return line;
            }
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public AsnLine updateAsnLine(Long id, AsnLine lineDetails) {
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                AsnLine existingLine = asnLineRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("AsnLine not found with id: " + id));
                
                validateAsnLine(lineDetails);
                
                // Update fields
                existingLine.setLineNumber(lineDetails.getLineNumber());
                existingLine.setItemNumber(lineDetails.getItemNumber());
                existingLine.setQuantity(lineDetails.getQuantity());
                existingLine.setStatus(lineDetails.getStatus());
                existingLine.setHeader(lineDetails.getHeader());
                
                return asnLineRepository.save(existingLine);
            },
            () -> {
                // Fallback: return the original line with error status
                lineDetails.setStatus("ERROR");
                return lineDetails;
            }
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void deleteAsnLine(Long id) {
        circuitBreakerService.executeVoidRepositoryOperation(
            () -> {
                if (!asnLineRepository.existsById(id)) {
                    throw new ResourceNotFoundException("AsnLine not found with id: " + id);
                }
                asnLineRepository.deleteById(id);
            },
            () -> {
                // Fallback: log error but don't throw exception
                throw new ValidationException("Repository service unavailable: Circuit breaker open");
            }
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public List<AsnLine> createAsnLines(List<AsnLine> lines) {
        if (lines == null || lines.isEmpty()) {
            return List.of();
        }

        long startTime = System.currentTimeMillis();
        
        try {
            return circuitBreakerService.executeRepositoryOperation(
                () -> {
                    // Validate all lines first
                    validateAsnLinesBatch(lines);
                    
                    // Ensure all lines have the same header and client
                    validateLineConsistency(lines);
                    
                    // Process in batches of optimal size
                    List<AsnLine> result = new ArrayList<>();
                    int batchSize = currentBatchSize.get();
                    
                    for (int i = 0; i < lines.size(); i += batchSize) {
                        int end = Math.min(i + batchSize, lines.size());
                        List<AsnLine> batch = lines.subList(i, end);
                        
                        // Save batch
                        result.addAll(asnLineRepository.saveAll(batch));
                        
                        // Adjust batch size based on performance
                        adjustBatchSize(System.currentTimeMillis() - startTime);
                    }
                    
                    return result;
                },
                () -> {
                    // Fallback: return lines with error status
                    lines.forEach(line -> line.setStatus("ERROR - Circuit breaker open"));
                    return lines;
                }
            );
        } finally {
            // Update metrics
            long processingTime = System.currentTimeMillis() - startTime;
            adjustBatchSize(processingTime);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AsnHeader> findByAsnNumber(String asnNumber) {
        return circuitBreakerService.executeRepositoryOperation(
            () -> asnHeaderRepository.findByAsnNumber(asnNumber),
            () -> Optional.<AsnHeader>empty() // Fallback: return empty optional
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public List<AsnHeader> createAsnHeaders(List<AsnHeader> headers) {
        if (headers == null || headers.isEmpty()) {
            return List.of();
        }

        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                // Validate all headers first
                validateAsnHeadersBatch(headers);
                
                // Save all headers in a batch
                return asnHeaderRepository.saveAll(headers);
            },
            () -> {
                // Fallback: return headers with error status
                headers.forEach(header -> header.setStatus("ERROR - Circuit breaker open"));
                return headers;
            }
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public List<AsnLine> updateAsnLines(List<AsnLine> lines) {
        if (lines == null || lines.isEmpty()) {
            return List.of();
        }

        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                // Validate all lines first
                validateAsnLinesBatch(lines);
                
                // Ensure all lines exist
                validateExistingLines(lines);
                
                // Update all lines in a batch
                return asnLineRepository.saveAll(lines);
            },
            () -> {
                // Fallback: return lines with error status
                lines.forEach(line -> line.setStatus("ERROR - Circuit breaker open"));
                return lines;
            }
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void deleteAsnHeaders(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        circuitBreakerService.executeRepositoryOperation(
            () -> {
                // Find all headers
                List<AsnHeader> headers = asnHeaderRepository.findAllById(ids);
                
                // Delete all associated lines first
                List<Long> headerIds = headers.stream()
                    .map(AsnHeader::getId)
                    .collect(Collectors.toList());
                    
                asnLineRepository.deleteByHeaderIdIn(headerIds);
                
                // Delete all headers
                asnHeaderRepository.deleteAllById(ids);
                
                return null;
            },
            () -> {
                log.error("Failed to delete ASN headers due to circuit breaker open");
                return null;
            }
        );
    }

    private void validateAsnHeader(AsnHeader header) {
        if (header == null) {
            throw new ValidationException("ASN Header cannot be null");
        }
        if (header.getClient() == null) {
            throw new ValidationException("Client must be specified for ASN Header");
        }
        // Add more validations as needed
    }

    private void validateAsnLine(AsnLine line) {
        if (line == null) {
            throw new ValidationException("ASN Line cannot be null");
        }
        if (line.getHeader() == null) {
            throw new ValidationException("Header must be specified for ASN Line");
        }
        if (line.getClient() == null) {
            throw new ValidationException("Client must be specified for ASN Line");
        }
        if (line.getLineNumber() == null || line.getLineNumber().trim().isEmpty()) {
            throw new ValidationException("Line number must be specified for ASN Line");
        }
        // Add more specific validations as needed
    }

    /**
     * Validates a batch of ASN headers
     * @param headers List of headers to validate
     * @throws ValidationException if validation fails
     */
    private void validateAsnHeadersBatch(List<AsnHeader> headers) {
        if (headers == null) {
            throw new ValidationException("ASN Headers list cannot be null");
        }

        // Check for duplicate ASN numbers within the batch
        Set<String> asnNumbers = new HashSet<>();
        for (AsnHeader header : headers) {
            if (header.getAsnNumber() != null && !asnNumbers.add(header.getAsnNumber())) {
                throw new ValidationException("Duplicate ASN number found in batch: " + header.getAsnNumber());
            }
        }

        // Validate each header
        headers.forEach(this::validateAsnHeader);
    }

    /**
     * Validates a batch of ASN lines
     * @param lines List of lines to validate
     * @throws ValidationException if validation fails
     */
    private void validateAsnLinesBatch(List<AsnLine> lines) {
        if (lines == null) {
            throw new ValidationException("ASN Lines list cannot be null");
        }

        // Check for duplicate line numbers within the batch
        Set<String> lineNumbers = new HashSet<>();
        for (AsnLine line : lines) {
            if (line.getLineNumber() != null && !lineNumbers.add(line.getLineNumber())) {
                throw new ValidationException("Duplicate line number found in batch: " + line.getLineNumber());
            }
        }

        // Validate each line
        lines.forEach(this::validateAsnLine);
    }

    /**
     * Validates that all lines in a batch belong to the same header and client
     * @param lines List of lines to validate
     * @throws ValidationException if validation fails
     */
    private void validateLineConsistency(List<AsnLine> lines) {
        if (lines.isEmpty()) {
            return;
        }

        AsnHeader header = lines.get(0).getHeader();
        if (header == null) {
            throw new ValidationException("Header must be specified for ASN Lines");
        }

        for (AsnLine line : lines) {
            if (!header.equals(line.getHeader())) {
                throw new ValidationException("All lines must belong to the same header");
            }
            if (!header.getClient().equals(line.getClient())) {
                throw new ValidationException("All lines must belong to the same client as the header");
            }
        }
    }

    /**
     * Validates that all lines in a batch exist in the database
     * @param lines List of lines to validate
     * @throws ValidationException if validation fails
     */
    private void validateExistingLines(List<AsnLine> lines) {
        List<Long> lineIds = lines.stream()
            .map(AsnLine::getId)
            .collect(Collectors.toList());
            
        List<AsnLine> existingLines = asnLineRepository.findAllById(lineIds);
        if (existingLines.size() != lines.size()) {
            throw new ValidationException("Some lines do not exist in the database");
        }
    }

    /**
     * Adjusts the batch size based on system load and processing time
     * @param processingTime Time taken to process the last batch in milliseconds
     */
    private void adjustBatchSize(long processingTime) {
        // Update metrics
        batchCount.incrementAndGet();
        totalProcessingTime.addAndGet(processingTime);
        lastBatchTime.set(Instant.now().toEpochMilli());

        // Calculate average processing time per item
        double avgTimePerItem = (double) totalProcessingTime.get() / (batchCount.get() * currentBatchSize.get());

        // Get system load
        double systemLoad = getSystemLoad();

        // Adjust batch size based on system load and processing time
        int newBatchSize = currentBatchSize.get();
        if (systemLoad > loadThreshold) {
            // Reduce batch size if system is under high load
            newBatchSize = Math.max(minBatchSize, currentBatchSize.get() - batchAdjustmentStep);
        } else if (avgTimePerItem < 10) { // If processing is fast
            // Increase batch size if processing is efficient
            newBatchSize = Math.min(maxBatchSize, currentBatchSize.get() + batchAdjustmentStep);
        }

        if (newBatchSize != currentBatchSize.get()) {
            log.info("Adjusting batch size from {} to {} (load: {}, avg time/item: {}ms)",
                currentBatchSize.get(), newBatchSize, systemLoad, avgTimePerItem);
            currentBatchSize.set(newBatchSize);
        }
    }

    /**
     * Gets the current system load (CPU usage)
     * @return System load as a value between 0 and 1
     */
    private double getSystemLoad() {
        try {
            com.sun.management.OperatingSystemMXBean osBean = 
                (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            return osBean.getSystemCpuLoad();
        } catch (Exception e) {
            log.warn("Failed to get system load: {}", e.getMessage());
            return 0.5; // Default to moderate load if we can't measure it
        }
    }
}
