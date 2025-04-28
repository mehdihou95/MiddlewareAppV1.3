package com.middleware.processor.service.interfaces;

import com.middleware.shared.model.OrderHeader;
import com.middleware.shared.model.OrderLine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing Order documents.
 */
public interface OrderService {
    /**
     * Create a new Order header.
     *
     * @param header The Order header to create
     * @return The created Order header
     */
    OrderHeader createOrderHeader(OrderHeader header);

    /**
     * Get an Order header by its ID.
     *
     * @param id The ID of the Order header
     * @return Optional containing the Order header if found
     */
    Optional<OrderHeader> getOrderHeaderById(Long id);

    /**
     * Get all Order headers with pagination.
     *
     * @param pageable The pagination information
     * @return Page of Order headers
     */
    Page<OrderHeader> getAllOrderHeaders(Pageable pageable);

    /**
     * Get all Order headers.
     *
     * @return List of all Order headers
     */
    List<OrderHeader> getAllOrderHeaders();

    /**
     * Get Order headers for a specific client with pagination.
     *
     * @param clientId The ID of the client
     * @param pageable The pagination information
     * @return Page of Order headers
     */
    Page<OrderHeader> getOrderHeadersByClient(Long clientId, Pageable pageable);

    /**
     * Get Order headers for a specific client with pagination.
     *
     * @param clientId The ID of the client
     * @param pageable The pagination information
     * @return Page of Order headers
     */
    Page<OrderHeader> getOrderHeadersByClient_Id(Long clientId, Pageable pageable);

    /**
     * Get Order headers for a specific client.
     *
     * @param clientId The ID of the client
     * @return List of Order headers for the client
     */
    List<OrderHeader> getOrderHeadersByClient(Long clientId);

    /**
     * Find an Order header by order number and client ID.
     *
     * @param orderNumber The order number
     * @param clientId The ID of the client
     * @return Optional containing the Order header if found
     */
    Optional<OrderHeader> findByOrderNumberAndClient_Id(String orderNumber, Long clientId);

    /**
     * Find Order headers by client ID and order date range with pagination.
     *
     * @param clientId The ID of the client
     * @param startDate The start date
     * @param endDate The end date
     * @param pageable The pagination information
     * @return Page of Order headers
     */
    Page<OrderHeader> findByClient_IdAndOrderDateDttmBetween(Long clientId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    /**
     * Update an Order header.
     *
     * @param id The ID of the Order header to update
     * @param header The updated Order header
     * @return The updated Order header
     */
    OrderHeader updateOrderHeader(Long id, OrderHeader headerDetails);

    /**
     * Delete an Order header.
     *
     * @param id The ID of the Order header to delete
     */
    void deleteOrderHeader(Long id);
    
    /**
     * Create a new Order line.
     *
     * @param line The Order line to create
     * @return The created Order line
     */
    OrderLine createOrderLine(OrderLine line);

    /**
     * Get an Order line by its ID.
     *
     * @param id The ID of the Order line
     * @return Optional containing the Order line if found
     */
    Optional<OrderLine> getOrderLineById(Long id);

    /**
     * Get all Order lines with pagination.
     *
     * @param pageable The pagination information
     * @return Page of Order lines
     */
    Page<OrderLine> getAllOrderLines(Pageable pageable);

    /**
     * Get Order lines by header ID with pagination.
     *
     * @param headerId The ID of the Order header
     * @param pageable The pagination information
     * @return Page of Order lines
     */
    Page<OrderLine> getOrderLinesByHeader(Long headerId, Pageable pageable);

    /**
     * Get Order lines by header ID.
     *
     * @param headerId The ID of the Order header
     * @return List of Order lines
     */
    List<OrderLine> getOrderLinesByHeader(Long headerId);

    /**
     * Get Order lines by header ID with pagination.
     *
     * @param headerId The ID of the Order header
     * @param pageable The pagination information
     * @return Page of Order lines
     */
    Page<OrderLine> getOrderLinesByHeader_Id(Long headerId, Pageable pageable);

    /**
     * Get Order lines by client ID with pagination.
     *
     * @param clientId The ID of the client
	 * @param headerId The ID of the ORDER header										   
     * @param pageable The pagination information
     * @return Page of Order lines
     */
    Page<OrderLine> getOrderLinesByClient_Id(Long clientId, Pageable pageable);

    /**
     * Find Order lines by client ID and header ID with pagination.
     *
     * @param clientId The ID of the client
     * @param headerId The ID of the Order header
     * @param pageable The pagination information
     * @return Page of Order lines
     */
    Page<OrderLine> findByClient_IdAndHeader_Id(Long clientId, Long headerId, Pageable pageable);

    /**
     * Find Order lines by client ID and item number with pagination.
     *
     * @param clientId The ID of the client
     * @param itemNumber The item number
     * @param pageable The pagination information
     * @return Page of Order lines
     */
    Page<OrderLine> findByClient_IdAndItemNumber(Long clientId, String itemNumber, Pageable pageable);

    /**
     * Find Order lines by client ID and status with pagination.
     *
     * @param clientId The ID of the client
     * @param status The status
     * @param pageable The pagination information
     * @return Page of Order lines
     */
    Page<OrderLine> findByClient_IdAndStatus(Long clientId, String status, Pageable pageable);

    /**
     * Find Order lines by client ID and quantity greater than with pagination.
     *
     * @param clientId The ID of the client
     * @param quantity The quantity threshold
     * @param pageable The pagination information
     * @return Page of Order lines
     */
    Page<OrderLine> findByClient_IdAndQuantityGreaterThan(Long clientId, Integer quantity, Pageable pageable);

    /**
     * Update an Order line.
     *
     * @param id The ID of the Order line to update
     * @param line The updated Order line
     * @return The updated Order line
     */
    OrderLine updateOrderLine(Long id, OrderLine lineDetails);

    /**
     * Delete an Order line.
     *
     * @param id The ID of the Order line to delete
     */
    void deleteOrderLine(Long id);

    /**
     * Create multiple Order lines.
     *
     * @param lines The list of Order lines to create
     * @return The list of created Order lines
     */
    List<OrderLine> createOrderLines(List<OrderLine> lines);

    /**
     * Find Order header by number.
     *
     * @param orderNumber The order number
     * @return Optional containing the Order header if found
     */
    Optional<OrderHeader> findByOrderNumber(String orderNumber);

    /**
     * Get Order lines by order ID with pagination.
     *
     * @param orderId The ID of the Order header
     * @param pageable The pagination information
     * @return Page of Order lines
     */
    Page<OrderLine> getOrderLinesByOrderId(Long orderId, Pageable pageable);
}