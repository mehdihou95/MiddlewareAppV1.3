package com.middleware.processor.service.strategy;

import com.middleware.processor.exception.ValidationException;
import com.middleware.processor.service.interfaces.OrderService;
import com.middleware.processor.service.interfaces.XmlValidationService;
import com.middleware.processor.service.util.TransformationService;
import com.middleware.processor.service.util.XmlProcessor;
import com.middleware.shared.model.Interface;
import com.middleware.shared.model.MappingRule;
import com.middleware.shared.model.OrderHeader;
import com.middleware.shared.model.OrderLine;
import com.middleware.shared.repository.MappingRuleRepository;
import com.middleware.shared.repository.ProcessedFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Strategy for processing Order documents.
 * Extends the base template to provide Order-specific processing logic.
 */
@Component
public class OrderDocumentProcessingStrategy extends BaseDocumentProcessingTemplate {

    private static final Logger log = LoggerFactory.getLogger(OrderDocumentProcessingStrategy.class);

    private static final String ORDER_ROOT_ELEMENT = "ORDER";

    @Autowired
    private OrderService orderService;

    @Override
    public String getDocumentType() {
        return ORDER_ROOT_ELEMENT;
    }

    @Override
    public boolean canHandle(String rootElement) {
        return ORDER_ROOT_ELEMENT.equals(rootElement);
    }

    @Override
    public String getName() {
        return "Order Processing Strategy";
    }

    @Override
    public int getPriority() {
        return 20; // Example priority, lower than ASN if ASN is more specific
    }

    /**
     * Performs Order-specific document processing.
     * Requires an existing transaction (MANDATORY propagation).
     *
     * @param document        The parsed XML document.
     * @param interfaceEntity The interface configuration.
     * @return The processed OrderHeader entity.
     * @throws Exception If processing fails.
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    protected Object processSpecificDocument(Document document, Interface interfaceEntity) throws Exception {
        log.debug("Starting Order specific processing for interface: {}", interfaceEntity.getName());

        // 1. Extract Header Data
        OrderHeader orderHeader = extractOrderHeader(document, interfaceEntity);
        log.debug("Order Header extracted: {}", orderHeader.getOrderNumber());

        // 2. Extract Line Item Data
        List<OrderLine> orderLines = extractOrderLines(document, interfaceEntity, orderHeader);
        log.debug("Extracted {} Order lines.", orderLines.size());

        // Save header first
        OrderHeader savedHeader = orderService.createOrderHeader(orderHeader);
        
        // Then create lines
        orderLines.forEach(line -> {
            line.setOrderHeader(savedHeader);
            orderService.createOrderLine(line);
        });
        
        log.info("Order Header and Lines saved successfully for Order: {}", savedHeader.getOrderNumber());

        return savedHeader;
    }

    /**
     * Extracts Order header data from the XML document based on mapping rules.
     * Requires an existing transaction (MANDATORY propagation).
     *
     * @param document        The XML document.
     * @param interfaceEntity The interface configuration.
     * @return The populated OrderHeader entity.
     * @throws Exception If extraction fails.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    protected OrderHeader extractOrderHeader(Document document, Interface interfaceEntity) throws Exception {
        OrderHeader orderHeader = new OrderHeader();
        orderHeader.setClient(interfaceEntity.getClient());
        orderHeader.setStatus("NEW");
        orderHeader.setOrderDateDttm(LocalDateTime.now());

        List<MappingRule> rules = getActiveMappingRules(interfaceEntity.getId());
        log.debug("Applying {} mapping rules for Order header.", rules.size());

        for (MappingRule rule : rules) {
            if (isHeaderRule(rule)) { // Assuming a method to identify header rules
                try {
                    // Use standardized sourceField
                    String xmlValue = xmlProcessor.evaluateXPath(document, rule.getSourceField());
                    if (xmlValue != null && !xmlValue.isEmpty()) {
                        String transformedValue = applyTransformation(xmlValue, rule);
                        // Use standardized targetField
                        setFieldValue(orderHeader, rule.getTargetField(), transformedValue);
                    } else if (rule.getRequired()) {
                        throw new ValidationException("Required header field missing: " + rule.getName() + " (XPath: " + rule.getSourceField() + ")");
                    } else if (rule.getDefaultValue() != null) {
                        // Use standardized targetField
                        setFieldValue(orderHeader, rule.getTargetField(), rule.getDefaultValue());
                    }
                } catch (Exception e) {
                    log.error("Error processing header rule 	{}	 for path 	{}	: {}", rule.getName(), rule.getSourceField(), e.getMessage());
                    if (rule.getRequired()) {
                        throw new ValidationException("Error processing required header field " + rule.getName() + ": " + e.getMessage(), e);
                    }
                }
            }
        }
        return orderHeader;
    }

    /**
     * Extracts Order line items from the XML document based on mapping rules.
     * Requires an existing transaction (MANDATORY propagation).
     *
     * @param document        The XML document.
     * @param interfaceEntity The interface configuration.
     * @param orderHeader     The parent OrderHeader entity.
     * @return List of populated OrderLine entities.
     * @throws Exception If extraction fails.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    protected List<OrderLine> extractOrderLines(Document document, Interface interfaceEntity, OrderHeader orderHeader) throws Exception {
        List<OrderLine> orderLines = new ArrayList<>();
        List<MappingRule> rules = getActiveMappingRules(interfaceEntity.getId());

        NodeList lineNodes = xmlProcessor.evaluateXPathForNodes(document, "//OrderLine"); // Using correct method name
        log.debug("Found {} line nodes in XML.", lineNodes.getLength());

        for (int i = 0; i < lineNodes.getLength(); i++) {
            Element lineElement = (Element) lineNodes.item(i);
            OrderLine line = new OrderLine();
            line.setOrderHeader(orderHeader);
            line.setClient(interfaceEntity.getClient());
            line.setStatus("NEW");
            line.setLineNumber(String.format("%03d", i + 1)); // Format line number as 3-digit string

            for (MappingRule rule : rules) {
                if (isLineRule(rule)) { // Assuming a method to identify line rules
                    try {
                        String xmlValue = xmlProcessor.evaluateXPath(lineElement, rule.getSourceField());
                        if (xmlValue != null && !xmlValue.isEmpty()) {
                            String transformedValue = applyTransformation(xmlValue, rule);
                            // Apply the transformed value to the line using reflection
                            setFieldValue(line, rule.getTargetField(), transformedValue);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to apply line mapping rule {}: {}", rule.getId(), e.getMessage());
                    }
                }
            }
            orderLines.add(line);
        }
        return orderLines;
    }

    // --- Helper Methods (Similar to AsnDocumentProcessingStrategy) ---

    private boolean isHeaderRule(MappingRule rule) {
        // Adapt logic based on how Order header rules are identified
        return rule.getTargetField() != null && !rule.getTargetField().contains("line"); // Simplified example
    }

    private boolean isLineRule(MappingRule rule) {
        // Adapt logic based on how Order line rules are identified
        return rule.getTargetField() != null && rule.getTargetField().contains("line"); // Simplified example
    }

    private String determineLineItemBasePath(Interface interfaceEntity) {
        // Adapt logic for Order documents
        // return interfaceEntity.getLineItemBasePath();
        return "/Outbound/Orders/Order/Lines/Line"; // Hardcoded example for Order
    }

    private String adjustXPathForLine(String fullXPath, String basePath) {
        if (fullXPath != null && fullXPath.startsWith(basePath)) {
            String relativePath = fullXPath.substring(basePath.length());
            return relativePath.startsWith("/") ? "." + relativePath : "./" + relativePath;
        }
        log.warn("XPath 	{}	 does not start with base path 	{}	. Assuming relative path.", fullXPath, basePath);
        return fullXPath.startsWith("/") ? "." + fullXPath : "./" + fullXPath;
    }

    /**
     * Sets a field value on an entity using reflection.
     * 
     * @param entity The entity to set the field on
     * @param fieldName The name of the field to set
     * @param value The value to set
     * @throws Exception If the field cannot be set
     */
    protected void setFieldValue(Object entity, String fieldName, String value) throws Exception {
        try {
            String setterName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            Method setter = entity.getClass().getMethod(setterName, String.class);
            setter.invoke(entity, value);
        } catch (Exception e) {
            log.error("Failed to set field {} with value {}: {}", fieldName, value, e.getMessage());
            throw e;
        }
    }

    @Override
    protected void performAdditionalValidations(Document document, Interface interfaceEntity) throws ValidationException {
        log.debug("Performing additional Order validations for interface: {}", interfaceEntity.getName());
        // Add Order-specific validations if needed
        log.debug("Additional Order validations passed.");
    }
}

