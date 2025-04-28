package com.middleware.shared.repository;

import com.middleware.shared.model.OrderLine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for OrderLine entities with circuit breaker support.
 */
@Repository
public interface OrderLineRepository extends BaseRepository<OrderLine> {

    /**
     * Find order lines by order ID
     */
    List<OrderLine> findByOrderHeader_Id(Long orderId);

    /**
     * Find order lines by order ID with pagination
     */
    Page<OrderLine> findByOrderHeader_Id(Long orderId, Pageable pageable);

    /**
     * Find order lines by client ID and order ID with pagination
     */
    @Query("SELECT l FROM OrderLine l WHERE l.client.id = :clientId AND l.orderHeader.id = :orderId")
    Page<OrderLine> findByClient_IdAndOrderHeader_Id(
        @Param("clientId") Long clientId,
        @Param("orderId") Long orderId,
        Pageable pageable);

    /**
     * Find order lines by client ID and item number with pagination
     */
    Page<OrderLine> findByClient_IdAndItemNumber(Long clientId, String itemNumber, Pageable pageable);

    /**
     * Find order lines by client ID and status with pagination
     */
    Page<OrderLine> findByClient_IdAndStatus(Long clientId, String status, Pageable pageable);

    /**
     * Find order lines by client ID and quantity greater than with pagination
     */
    Page<OrderLine> findByClient_IdAndQuantityGreaterThan(Long clientId, Integer quantity, Pageable pageable);

    /**
     * Find order lines by client ID and order ID
     */
    @Query("SELECT l FROM OrderLine l WHERE l.client.id = :clientId AND l.orderHeader.id = :orderId")
    List<OrderLine> findByClient_IdAndOrderHeader_Id(
        @Param("clientId") Long clientId,
        @Param("orderId") Long orderId);

    /**
     * Find order lines by client ID and item number
     */
    List<OrderLine> findByClient_IdAndItemNumber(Long clientId, String itemNumber);

    /**
     * Find order lines by client ID and status
     */
    List<OrderLine> findByClient_IdAndStatus(Long clientId, String status);

    /**
     * Find order lines by client ID and quantity greater than
     */
    List<OrderLine> findByClient_IdAndQuantityGreaterThan(Long clientId, Integer quantity);

    /**
     * Find Order line by ID and client ID
     */
    Optional<OrderLine> findByIdAndClient_Id(Long id, Long clientId);

    /**
     * Find Order lines by client ID
     */
    List<OrderLine> findByClient_Id(Long clientId);

    /**
     * Find Order lines by order number and client ID
     */
    @Query("SELECT l FROM OrderLine l WHERE l.orderHeader.orderNumber = :orderNumber AND l.orderHeader.client.id = :clientId")
    List<OrderLine> findByOrderHeader_OrderNumberAndClient_Id(String orderNumber, Long clientId);
} 