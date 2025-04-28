package com.middleware.processor.service.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Service for monitoring client performance metrics.
 */
public interface ClientPerformanceMonitor {
    /**
     * Get performance metrics for a specific client.
     *
     * @param clientId The ID of the client
     * @param startDate The start date for the metrics
     * @param endDate The end date for the metrics
     * @return Map of performance metrics
     */
    Map<String, Object> getClientMetrics(Long clientId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get performance metrics for all clients with pagination.
     *
     * @param pageable The pagination information
     * @param startDate The start date for the metrics
     * @param endDate The end date for the metrics
     * @return Page of client performance metrics
     */
    Page<Map<String, Object>> getAllClientMetrics(Pageable pageable, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get performance alerts for a specific client.
     *
     * @param clientId The ID of the client
     * @param pageable The pagination information
     * @return Page of performance alerts
     */
    Page<Map<String, Object>> getClientAlerts(Long clientId, Pageable pageable);

    /**
     * Get performance alerts for all clients with pagination.
     *
     * @param pageable The pagination information
     * @return Page of performance alerts
     */
    Page<Map<String, Object>> getAllClientAlerts(Pageable pageable);
} 
