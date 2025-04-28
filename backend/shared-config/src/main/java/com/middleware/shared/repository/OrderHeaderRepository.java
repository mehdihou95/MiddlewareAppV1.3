package com.middleware.shared.repository;

import com.middleware.shared.model.OrderHeader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for OrderHeader entities with circuit breaker support.
 */
@Repository
public interface OrderHeaderRepository extends BaseRepository<OrderHeader> {
    
    /**
     * Find order by ID and client ID
     */
    @Query("SELECT h FROM OrderHeader h WHERE h.id = :orderId AND h.client.id = :clientId")
    Optional<OrderHeader> findByIdAndClient_Id(
        @Param("orderId") Long orderId,
        @Param("clientId") Long clientId);
    
    /**
     * Find order headers by client ID with pagination
     */
    @Query("SELECT h FROM OrderHeader h WHERE h.client.id = :clientId")
    Page<OrderHeader> findByClient_Id(
        @Param("clientId") Long clientId,
        Pageable pageable);
    
    /**
     * Find order header by order number and client ID
     */
    @Query("SELECT h FROM OrderHeader h WHERE h.orderNumber = :orderNumber AND h.client.id = :clientId")
    Optional<OrderHeader> findByOrderNumberAndClient_Id(
        @Param("orderNumber") String orderNumber,
        @Param("clientId") Long clientId);
    
    /**
     * Find order headers by client ID and date range with pagination
     */
    @Query("SELECT h FROM OrderHeader h WHERE h.client.id = :clientId AND h.orderDateDttm BETWEEN :startDate AND :endDate")
    Page<OrderHeader> findByClient_IdAndOrderDateDttmBetween(
        @Param("clientId") Long clientId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable);
    
    /**
     * Find order headers by client ID and date range
     */
    @Query("SELECT h FROM OrderHeader h WHERE h.client.id = :clientId AND h.orderDateDttm BETWEEN :startDate AND :endDate")
    List<OrderHeader> findAllByClient_IdAndOrderDateDttmBetween(
        @Param("clientId") Long clientId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find latest order headers by client ID
     */
    @Query("SELECT h FROM OrderHeader h WHERE h.client.id = :clientId ORDER BY h.createdAt DESC")
    List<OrderHeader> findLatestHeaders(@Param("clientId") Long clientId);

    /**
     * Find order headers by client ID and order date
     */
    @Query("SELECT h FROM OrderHeader h WHERE h.client.id = :clientId AND h.orderDateDttm = :orderDate")
    List<OrderHeader> findByClient_IdAndOrderDateDttm(
        @Param("clientId") Long clientId,
        @Param("orderDate") String orderDate);

    /**
     * Find order headers by client ID and status
     */
    List<OrderHeader> findByClient_IdAndStatus(Long clientId, String status);

    /**
     * Find order headers by client ID and status with pagination
     */
    Page<OrderHeader> findByClient_IdAndStatus(Long clientId, String status, Pageable pageable);

    /**
     * Find order headers by status with pagination
     */
    Page<OrderHeader> findByStatus(String status, Pageable pageable);

    /**
     * Find order header by order number
     */
    Optional<OrderHeader> findByOrderNumber(String orderNumber);
} 