package com.middleware.processor.service.factory;

import com.middleware.shared.model.AsnHeader;
import com.middleware.shared.model.AsnLine;
import com.middleware.shared.model.Client;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Factory service for creating ASN entities with default values.
 * Centralizes the initialization logic for ASN headers and lines.
 */
@Service
public class AsnFactory {
    
    // Counter for default line numbers when no mapping exists
    private final AtomicInteger defaultLineCounter = new AtomicInteger(0);
    
    /**
     * Create a default AsnHeader with standard values.
     * 
     * @param client The client entity
     * @return A new AsnHeader with default values
     */
    public AsnHeader createDefaultHeader(Client client) {
        // Reset line counter for new header
        defaultLineCounter.set(0);
        
        AsnHeader header = new AsnHeader();
        header.setClient(client);
        
        // Boolean flags
        header.setHasImportError(false);
        header.setHasSoftCheckError(false);
        header.setHasAlerts(false);
        header.setIsCogiGenerated(false);
        header.setIsCancelled(false);
        header.setIsClosed(false);
        header.setIsGift(false);
        header.setReceiptVariance(false);
        
        // String values
        header.setIsWhseTransfer("0");
        header.setStatus("NEW");
        header.setAsnNumber("DEFAULT");
        
        // Numeric values
        header.setAsnLevel(1);
        header.setAsnType(1);
        header.setQualityAuditPercent(BigDecimal.ZERO);
        header.setAsnPriority(0);
        header.setScheduleAppt(0);
        header.setCreatedSourceType(0);
        header.setLastUpdatedSourceType(0);
        
        // Date values
        header.setReceiptDttm(new java.text.SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        
        return header;
    }
    
    /**
     * Create a default AsnLine with standard values.
     * If no line number mapping exists, provides an auto-incrementing number.
     * 
     * @param header The parent AsnHeader
     * @param client The client entity
     * @return A new AsnLine with default values
     */
    public AsnLine createDefaultLine(AsnHeader header, Client client) {
        AsnLine line = new AsnLine();
        
        // Parent references
        line.setHeader(header);
        line.setClient(client);
        
        // Status and type values
        line.setStatus("NEW");
        line.setAsnDetailStatus(4);
        line.setIsCancelled(0);
        
        // Numeric values
        line.setQtyConvFactor(BigDecimal.ONE);
        line.setQuantity(0);
        line.setCreatedSourceType(1);
        line.setLastUpdatedSourceType(1);
        
        // String values
        line.setUnitOfMeasure("EA");
        
        // Set default line number (will be overwritten if mapping exists)
        line.setLineNumber(String.valueOf(defaultLineCounter.incrementAndGet()));
        
        return line;
    }
} 