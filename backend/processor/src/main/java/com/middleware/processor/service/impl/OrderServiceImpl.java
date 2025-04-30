package com.middleware.processor.service.impl;

import com.middleware.processor.exception.ValidationException;
import com.middleware.shared.model.OrderHeader;
import com.middleware.shared.model.OrderLine;
import com.middleware.shared.repository.OrderHeaderRepository;
import com.middleware.shared.repository.OrderLineRepository;
import com.middleware.processor.service.interfaces.OrderService;
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
 * Implementation of OrderService with Circuit Breaker pattern.
 * Provides operations for managing Order documents.
 */
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderHeaderRepository orderHeaderRepository;
    
    @Autowired
    private OrderLineRepository orderLineRepository;
    
    @Autowired
    private CircuitBreakerService circuitBreakerService;

    @Override
    @Transactional(readOnly = true)
    public List<OrderHeader> getAllOrderHeaders() {
        return circuitBreakerService.executeRepositoryOperation(
            () -> orderHeaderRepository.findAll(),
            () -> new ArrayList<OrderHeader>() // Fallback: return empty list
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderHeader> getAllOrderHeaders(Pageable pageable) {
        return circuitBreakerService.executeRepositoryOperation(
            () -> orderHeaderRepository.findAll(pageable),
            () -> Page.empty(pageable) // Fallback: return empty page
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrderHeader> getOrderHeaderById(Long id) {
        return circuitBreakerService.executeRepositoryOperation(
            () -> orderHeaderRepository.findById(id),
            Optional::empty // Fallback: return empty optional
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderHeader> getOrderHeadersByClient(Long clientId) {
        return circuitBreakerService.executeRepositoryOperation(
            () -> orderHeaderRepository.findByClient_Id(clientId),
            () -> new ArrayList<OrderHeader>() // Fallback: return empty list
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderHeader> getOrderHeadersByClient(Long clientId, Pageable pageable) {
        return circuitBreakerService.executeRepositoryOperation(
            () -> orderHeaderRepository.findByClient_Id(clientId, pageable),
            () -> Page.empty(pageable) // Fallback: return empty page
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderHeader> getOrderHeadersByClient_Id(Long clientId, Pageable pageable) {
        return circuitBreakerService.<Page<OrderHeader>>executeRepositoryOperation(
            () -> orderHeaderRepository.findByClient_Id(clientId, pageable),
            () -> Page.empty(pageable)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrderHeader> findByOrderNumberAndClient_Id(String orderNumber, Long clientId) {
        return circuitBreakerService.executeRepositoryOperation(
            () -> orderHeaderRepository.findByOrderNumberAndClient_IdWithClientAndLines(orderNumber, clientId),
            Optional::empty // Fallback: return empty optional
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderHeader> findByClient_IdAndOrderDateDttmBetween(Long clientId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
		return circuitBreakerService.executeRepositoryOperation(													
            () -> orderHeaderRepository.findByClient_IdAndOrderDateDttmBetween(clientId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59), pageable),
            () -> Page.empty(pageable) // Fallback: return empty page
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public OrderHeader createOrderHeader(OrderHeader header) {
        validateOrderHeader(header);
        return circuitBreakerService.executeRepositoryOperation(
		 () -> {
                // Save the header with entity graph
                OrderHeader savedHeader = orderHeaderRepository.save(header);
                
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
    public OrderHeader updateOrderHeader(Long id, OrderHeader headerDetails) {
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                OrderHeader existingHeader = orderHeaderRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("OrderHeader not found with id: " + id));
                
                validateOrderHeader(headerDetails);
                
                // Update fields
                existingHeader.setOrderNumber(headerDetails.getOrderNumber());
                existingHeader.setCreationType(headerDetails.getCreationType());
                existingHeader.setBusinessPartnerId(headerDetails.getBusinessPartnerId());
                existingHeader.setBusinessPartnerName(headerDetails.getBusinessPartnerName());
                existingHeader.setOrderDateDttm(headerDetails.getOrderDateDttm());
                existingHeader.setOrderReconDttm(headerDetails.getOrderReconDttm());
                existingHeader.setStatus(headerDetails.getStatus());
                existingHeader.setNotes(headerDetails.getNotes());
                existingHeader.setClient(headerDetails.getClient());
                
                return orderHeaderRepository.save(existingHeader);
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
    public void deleteOrderHeader(Long id) {
		
        circuitBreakerService.executeVoidRepositoryOperation(
            () -> {
                if (!orderHeaderRepository.existsById(id)) {
                    throw new ResourceNotFoundException("OrderHeader not found with id: " + id);
                }
                orderHeaderRepository.deleteById(id);
            },
            () -> {
                // Fallback: log error but don't throw exception
                throw new ValidationException("Repository service unavailable: Circuit breaker open");
            }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderLine> getOrderLinesByHeader(Long headerId) {
        return circuitBreakerService.<List<OrderLine>>executeRepositoryOperation(
            () -> orderLineRepository.findByOrderHeader_Id(headerId),
            () -> new ArrayList<OrderLine>()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderLine> getOrderLinesByHeader(Long headerId, Pageable pageable) {
        return circuitBreakerService.executeRepositoryOperation(
            () -> orderLineRepository.findByOrderHeader_Id(headerId, pageable),
            () -> Page.empty(pageable) // Fallback: return empty page
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderLine> getOrderLinesByHeader_Id(Long headerId, Pageable pageable) {
        return circuitBreakerService.executeRepositoryOperation(
            () -> orderLineRepository.findByOrderHeader_Id(headerId, pageable),
            () -> Page.empty(pageable)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderLine> getAllOrderLines(Pageable pageable) {
        return circuitBreakerService.executeRepositoryOperation(
            () -> orderLineRepository.findAll(pageable),
            () -> Page.empty(pageable)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderLine> getOrderLinesByClient_Id(Long clientId, Pageable pageable) {
      return circuitBreakerService.executeRepositoryOperation(
            () -> orderLineRepository.findByClient_Id(clientId, pageable),
            () -> Page.empty(pageable) // Fallback: return empty page
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderLine> getOrderLinesByOrderId(Long orderId, Pageable pageable) {
        return circuitBreakerService.executeRepositoryOperation(
            () -> orderLineRepository.findByOrderHeader_Id(orderId, pageable),
            () -> Page.empty(pageable) // Fallback: return empty page
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderLine> findByClient_IdAndHeader_Id(Long clientId, Long headerId, Pageable pageable) {
        return circuitBreakerService.executeRepositoryOperation(
            () -> orderLineRepository.findByClient_IdAndOrderHeader_Id(clientId, headerId, pageable),
            () -> Page.empty(pageable) // Fallback: return empty page
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderLine> findByClient_IdAndItemNumber(Long clientId, String itemNumber, Pageable pageable) {
        return circuitBreakerService.executeRepositoryOperation(
            () -> orderLineRepository.findByClient_IdAndItemNumber(clientId, itemNumber, pageable),
            () -> Page.empty(pageable) // Fallback: return empty page
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderLine> findByClient_IdAndStatus(Long clientId, String status, Pageable pageable) {
        return circuitBreakerService.executeRepositoryOperation(
            () -> orderLineRepository.findByClient_IdAndStatus(clientId, status, pageable),
            () -> Page.empty(pageable) // Fallback: return empty page
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderLine> findByClient_IdAndQuantityGreaterThan(Long clientId, Integer quantity, Pageable pageable) {
        return circuitBreakerService.executeRepositoryOperation(
            () -> orderLineRepository.findByClient_IdAndQuantityGreaterThan(clientId, quantity, pageable),
            () -> Page.empty(pageable) // Fallback: return empty page
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public OrderLine createOrderLine(OrderLine line) {
        validateOrderLine(line);
        return circuitBreakerService.executeRepositoryOperation(
            () -> orderLineRepository.save(line),
            () -> {
                // Fallback: return the original line with error status
                line.setStatus("ERROR");
                return line;
            }
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public OrderLine updateOrderLine(Long id, OrderLine lineDetails) {
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                OrderLine existingLine = orderLineRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("OrderLine not found with id: " + id));
                
                validateOrderLine(lineDetails);
                
                // Update fields
                existingLine.setLineNumber(lineDetails.getLineNumber());
                existingLine.setItemNumber(lineDetails.getItemNumber());
                existingLine.setItemDescription(lineDetails.getItemDescription());
                existingLine.setQuantity(lineDetails.getQuantity());
                existingLine.setUnitOfMeasure(lineDetails.getUnitOfMeasure());
                existingLine.setStatus(lineDetails.getStatus());
                existingLine.setNotes(lineDetails.getNotes());
                existingLine.setOrderHeader(lineDetails.getOrderHeader());
                
                return orderLineRepository.save(existingLine);
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
    public void deleteOrderLine(Long id) {
        circuitBreakerService.executeVoidRepositoryOperation(
            () -> {
                if (!orderLineRepository.existsById(id)) {
                    throw new ResourceNotFoundException("OrderLine not found with id: " + id);
                }
                orderLineRepository.deleteById(id);
            },
            () -> {
                // Fallback: log error but don't throw exception
                throw new ValidationException("Repository service unavailable: Circuit breaker open");
            }
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public List<OrderLine> createOrderLines(List<OrderLine> lines) {
		if (lines == null || lines.isEmpty()) {
            return List.of();
        }
		
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                         // Validate all lines first
                lines.forEach(this::validateOrderLine);
                
                // Ensure all lines have the same header and client
                OrderHeader header = lines.get(0).getOrderHeader();
                if (header == null) {
                    throw new ValidationException("Header must be specified for ASN Lines");
                }
                
                lines.forEach(line -> {
                    if (!header.equals(line.getOrderHeader())) {
                        throw new ValidationException("All lines must belong to the same header");
                    }
                    if (!header.getClient().equals(line.getClient())) {
                        throw new ValidationException("All lines must belong to the same client as the header");
                    }
                });
                
                // Save all lines in a batch					
                return orderLineRepository.saveAll(lines);
            },
            () -> {
                // Fallback: return the original lines with error status
                lines.forEach(line -> line.setStatus("ERROR - Circuit breaker open"));
                return lines;
            }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrderHeader> findByOrderNumber(String orderNumber) {
        return circuitBreakerService.executeRepositoryOperation(
            () -> orderHeaderRepository.findByOrderNumber(orderNumber),
            () -> Optional.<OrderHeader>empty() // Fallback: return empty optional
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrderLine> getOrderLineById(Long id) {
        return circuitBreakerService.executeRepositoryOperation(
            () -> orderLineRepository.findById(id),
            Optional::empty
        );
    }

    private void validateOrderHeader(OrderHeader orderHeader) {
        if (orderHeader.getOrderNumber() == null || orderHeader.getOrderNumber().trim().isEmpty()) {
            throw new ValidationException("Order number is required");
        }
        if (orderHeader.getClient() == null || orderHeader.getClient().getId() == null) {
            throw new ValidationException("Client is required");
        }
    }

    private void validateOrderLine(OrderLine orderLine) {
        if (orderLine.getOrderHeader() == null || orderLine.getOrderHeader().getId() == null) {
            throw new ValidationException("Order header is required");
        }
        if (orderLine.getOrderHeader() == null) {
            throw new ValidationException("Header must be specified for ASN Line");
        }
        if (orderLine.getClient() == null) {
            throw new ValidationException("Client must be specified for ASN Line");
        }
        if (orderLine.getItemNumber() == null || orderLine.getItemNumber().trim().isEmpty()) {
            throw new ValidationException("Item number is required");
        }
    }
}
