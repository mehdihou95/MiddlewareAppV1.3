package com.middleware.processor.service.impl;

import com.middleware.processor.exception.ValidationException;
import com.middleware.shared.model.AsnHeader;
import com.middleware.shared.model.AsnLine;
import com.middleware.shared.repository.AsnHeaderRepository;
import com.middleware.shared.repository.AsnLineRepository;
import com.middleware.processor.service.interfaces.AsnService;
import com.middleware.shared.service.util.CircuitBreakerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import com.middleware.shared.exception.ResourceNotFoundException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

/**
 * Implementation of AsnService with Circuit Breaker pattern.
 * Provides operations for managing ASN documents.
 */
@Service
public class AsnServiceImpl implements AsnService {

    @Autowired
    private AsnHeaderRepository asnHeaderRepository;
    
    @Autowired
    private AsnLineRepository asnLineRepository;
    
    @Autowired
    private CircuitBreakerService circuitBreakerService;

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
            () -> Page.empty(pageable) // Fallback: return empty page
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
        
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                // Validate all lines first
                lines.forEach(this::validateAsnLine);
                
                // Ensure all lines have the same header and client
                AsnHeader header = lines.get(0).getHeader();
                if (header == null) {
                    throw new ValidationException("Header must be specified for ASN Lines");
                }
                
                lines.forEach(line -> {
                    if (!header.equals(line.getHeader())) {
                        throw new ValidationException("All lines must belong to the same header");
                    }
                    if (!header.getClient().equals(line.getClient())) {
                        throw new ValidationException("All lines must belong to the same client as the header");
                    }
                });
                
                // Save all lines in a batch
                return asnLineRepository.saveAll(lines);
            },
            () -> {
                lines.forEach(line -> line.setStatus("ERROR - Circuit breaker open"));
                return lines;
            }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AsnHeader> findByAsnNumber(String asnNumber) {
        return circuitBreakerService.executeRepositoryOperation(
            () -> asnHeaderRepository.findByAsnNumber(asnNumber),
            () -> Optional.<AsnHeader>empty() // Fallback: return empty optional
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
}
