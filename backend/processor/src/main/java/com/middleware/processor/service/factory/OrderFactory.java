package com.middleware.processor.service.factory;

import com.middleware.shared.model.OrderHeader;
import com.middleware.shared.model.OrderLine;
import com.middleware.shared.model.Client;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Factory service for creating Order entities with default values.
 * Centralizes the initialization logic for Order headers and lines.
 */
@Service
public class OrderFactory {
    // Counter for default line numbers when no mapping exists
    private final AtomicInteger defaultLineCounter = new AtomicInteger(0);

    /**
     * Create a default OrderHeader with standard values.
     * 
	 
     * @param client The client entity
     * @return A new OrderHeader with default values
     */
    public OrderHeader createDefaultHeader(Client client) {
        // Reset line counter for new header
        defaultLineCounter.set(0);

        OrderHeader header = new OrderHeader();
        header.setClient(client);
        header.setStatus("NEW");
        header.setOrderNumber("DEFAULT");
        header.setCreationType("DEFAULT");
        header.setOrderDateDttm(LocalDateTime.now());
        header.setCreatedDttm(LocalDateTime.now());
        header.setLastUpdatedDttm(LocalDateTime.now());
        return header;
    }

    /**
     * Create a default OrderLine with standard values.
     * If no line number mapping exists, provides an auto-incrementing number.
     *
     * @param header The parent OrderHeader
     * @param client The client entity
     * @return A new OrderLine with default values
     */
    public OrderLine createDefaultLine(OrderHeader header, Client client) {
        OrderLine line = new OrderLine();
        line.setOrderHeader(header);
        line.setClient(client);
        line.setStatus("NEW");
        line.setLineNumber(String.valueOf(defaultLineCounter.incrementAndGet()));
        line.setQuantity(0);
        line.setUnitOfMeasure("EA");
        line.setOrderDetailStatus(4);
        line.setCreatedSourceType(1);
        line.setLastUpdatedSourceType(1);
        return line;
    }
} 
