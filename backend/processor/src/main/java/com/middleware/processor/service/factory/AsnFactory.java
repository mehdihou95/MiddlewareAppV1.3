package com.middleware.processor.service.factory;

import com.middleware.shared.model.AsnHeader;
import com.middleware.shared.model.AsnLine;
import com.middleware.shared.model.Client;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.LocalDateTime;

/**
 * Factory service for creating ASN entities with default values.
 * Provides defaults ONLY for non-nullable fields when:
 * 1. No mapping rule exists for the field
 * 2. Mapping rule exists but XML value is null
 */
@Service
public class AsnFactory {
    
    @Value("${asn.defaults.status:NEW}")
    private String defaultStatus;
    
    @Value("${asn.defaults.asn-number:DEFAULT}")
    private String defaultAsnNumber;
    
    @Value("${asn.defaults.asn-level:1}")
    private Integer defaultAsnLevel;
    
    @Value("${asn.defaults.asn-type:1}")
    private Integer defaultAsnType;
    
    @Value("${asn.defaults.quality-audit-percent:0}")
    private BigDecimal defaultQualityAuditPercent;
    
    @Value("${asn.defaults.asn-priority:0}")
    private Integer defaultAsnPriority;
    
    @Value("${asn.defaults.schedule-appt:0}")
    private Integer defaultScheduleAppt;
    
    @Value("${asn.defaults.source-type:0}")
    private Integer defaultSourceType;
    
    @Value("${asn.defaults.whse-transfer:0}")
    private String defaultWhseTransfer;
    
    @Value("${asn.defaults.line.unit-of-measure:EA}")
    private String defaultUnitOfMeasure;
    
    @Value("${asn.defaults.line.asn-detail-status:4}")
    private Integer defaultAsnDetailStatus;
    
    @Value("${asn.defaults.line.qty-conv-factor:1}")
    private BigDecimal defaultQtyConvFactor;
    
    // Counter for default line numbers when no mapping exists
    private final AtomicInteger defaultLineCounter = new AtomicInteger(0);
    
    /**
     * Create an AsnHeader with defaults for non-nullable fields.
     * These defaults are used only when no mapping rule exists or when XML value is null.
     * 
     * @param client The client entity (required)
     * @return A new AsnHeader with defaults for non-nullable fields
     */
    public AsnHeader createDefaultHeader(Client client) {
        // Reset line counter for new header
        defaultLineCounter.set(0);
        
        AsnHeader header = new AsnHeader();
        
        // Required relationship
        header.setClient(client);
        
        // Non-nullable fields from schema
        header.setStatus("NEW");                    // Required status
        header.setAsnNumber("PENDING");             // Required ASN number
        header.setAsnType(defaultAsnType);          // Required type (from properties)
        header.setAsnLevel(defaultAsnLevel);        // Required level (from properties)
        header.setAsnPriority(defaultAsnPriority);  // Required priority (from properties)
        header.setScheduleAppt(defaultScheduleAppt); // Required schedule appointment (from properties)
        header.setReceiptDttm(LocalDateTime.now()); // Required receipt date
        header.setQualityAuditPercent(defaultQualityAuditPercent); // Required quality audit percent
        header.setIsWhseTransfer(defaultWhseTransfer); // Required warehouse transfer flag
        
        // Required audit fields
        header.setCreatedSourceType(defaultSourceType);     // Required source type
        header.setLastUpdatedSourceType(defaultSourceType); // Required source type
        header.setCreatedAt(LocalDateTime.now());
        header.setUpdatedAt(LocalDateTime.now());
        
        // Required boolean flags
        header.setHasImportError(false);
        header.setHasSoftCheckError(false);
        header.setHasAlerts(false);
        header.setIsCogiGenerated(false);
        header.setIsCancelled(false);
        header.setIsClosed(false);
        header.setIsGift(false);
        header.setReceiptVariance(false);
        
        return header;
    }
    
    /**
     * Create an AsnLine with defaults for non-nullable fields.
     * These defaults are used only when no mapping rule exists or when XML value is null.
     * 
     * @param header The parent AsnHeader (required)
     * @param client The client entity (required)
     * @return A new AsnLine with defaults for non-nullable fields
     */
    public AsnLine createDefaultLine(AsnHeader header, Client client) {
        AsnLine line = new AsnLine();
        
        // Required relationships
        line.setHeader(header);
        line.setClient(client);
        
        // Non-nullable fields from schema
        line.setStatus("NEW");                      // Required status
        line.setLineNumber(String.format("%03d", defaultLineCounter.incrementAndGet())); // Required line number
        line.setQuantity(0);                        // Required quantity
        line.setUnitOfMeasure(defaultUnitOfMeasure); // Required UOM (from properties)
        line.setAsnDetailStatus(defaultAsnDetailStatus); // Required detail status (from properties)
        line.setIsCancelled(0);                     // Required cancelled flag
        line.setQtyConvFactor(defaultQtyConvFactor); // Required conversion factor
        
        // Required audit fields
        line.setCreatedSourceType(defaultSourceType);     // Required source type
        line.setLastUpdatedSourceType(defaultSourceType); // Required source type
        line.setCreatedAt(LocalDateTime.now());
        line.setUpdatedAt(LocalDateTime.now());
        
        return line;
    }
} 
