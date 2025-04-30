package com.middleware.processor.service.strategy;

import com.middleware.processor.exception.ValidationException;
import com.middleware.processor.service.factory.OrderFactory;
import com.middleware.processor.service.interfaces.OrderService;
import com.middleware.shared.model.*;
import com.middleware.shared.repository.ClientRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Concrete strategy for processing ORDER documents.
 * Extends the BaseDocumentProcessingTemplate for common functionality.
 * Uses standardized field and line processing methods from the base class.
 */
@Slf4j
@Service // Changed from @Component for consistency with AsnStrategy
public class OrderDocumentProcessingStrategy extends BaseDocumentProcessingTemplate {

    private static final String ORDER_ROOT_ELEMENT = "ORDER";
    private static final String DEFAULT_ORDER_LINE_XPATH = "//OrderLine"; // Default XPath if derivation fails

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderFactory orderFactory;

    @Autowired
    private ClientRepository clientRepository;

    @Override
    public boolean canHandle(String rootElement) {
        return ORDER_ROOT_ELEMENT.equals(rootElement);
    }

    @Override
    public String getDocumentType() {
        return ORDER_ROOT_ELEMENT;
    }

    @Override
    public String getName() {
        return "Order Processing Strategy";
    }

    @Override
    public int getPriority() {
        return 20; // Example priority
    }

    /**
     * Processes the specific ORDER document content.
     * Creates header and line entities based on mapping rules.
     * Runs within the transaction started by the base class processDocument method.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED) // Changed from MANDATORY
    protected Object processSpecificDocument(Document document, Interface interfaceEntity) throws Exception {
        log.debug("Processing ORDER specific document for interface: {}", interfaceEntity.getName());

        // Fetch a fresh Client instance to avoid session issues
        Client freshClient = clientRepository.findById(interfaceEntity.getClient().getId())
            .orElseThrow(() -> new ValidationException("Client not found with ID: " + interfaceEntity.getClient().getId()));

        List<MappingRule> allRules = getActiveMappingRules(interfaceEntity.getId());

        // 1. Process Header
        OrderHeader orderHeader = createAndProcessHeader(document, freshClient, allRules);

        // Save header first to get ID
        OrderHeader savedOrderHeader = orderService.createOrderHeader(orderHeader);
        if (savedOrderHeader == null || savedOrderHeader.getId() == null) {
            throw new ValidationException("Failed to save Order Header or generate ID.");
        }
        log.debug("Saved Order Header with ID: {}", savedOrderHeader.getId());

        // 2. Process Lines using the generic line processing method
        List<OrderLine> orderLines = processLineItems(
            document,
            savedOrderHeader, // Pass the saved header with ID
            allRules,
            DEFAULT_ORDER_LINE_XPATH,
            OrderHeader::getClient, // Function to get Client from Header
            (header, client) -> orderFactory.createDefaultLine(header, client), // Line factory function
            (line, element) -> applyOrderLineRules(line, element, allRules) // Rule application logic for one line
        );

        // Save lines if any were processed
        if (!orderLines.isEmpty()) {
            log.debug("Saving {} processed Order lines.", orderLines.size());
            orderService.createOrderLines(orderLines);
        }

        log.info("Successfully processed ORDER document for interface: {}", interfaceEntity.getName());
        return savedOrderHeader; // Return the processed header entity
    }

    /**
     * Creates the OrderHeader entity and applies header-level mapping rules.
     */
    private OrderHeader createAndProcessHeader(Document document, Client client, List<MappingRule> allRules) throws Exception {
        OrderHeader header = orderFactory.createDefaultHeader(client);

        List<MappingRule> headerRules = allRules.stream()
            .filter(rule -> rule.getTargetLevel() == TargetLevel.HEADER)
            .collect(Collectors.toList());

        log.debug("Applying {} header mapping rules.", headerRules.size());

        for (MappingRule rule : headerRules) {
            // Evaluate XPath relative to the document root for header fields
            String rawValue = xmlProcessor.evaluateXPath(document, rule.getSourceField());
            // Use the standardized method from the base class to apply the rule
            applyRuleToField(header, rule, rawValue);
        }

        // Perform any final header validation if needed
        validateOrderHeader(header);

        return header;
    }

    /**
     * Applies mapping rules specific to an OrderLine entity within the context of its XML element.
     * This method is passed as a lambda to the base class processLineItems.
     */
    private void applyOrderLineRules(OrderLine line, Element lineElement, List<MappingRule> allRules) {
        List<MappingRule> lineLevelRules = allRules.stream()
            .filter(rule -> rule.getTargetLevel() == TargetLevel.LINE)
            .collect(Collectors.toList());

        // Apply default values first (optional, could be in factory)
        // applyDefaultValues(line, lineLevelRules); // If needed

        for (MappingRule rule : lineLevelRules) {
            try {
                // Determine the relative XPath for the field within the line element
                String relativeXPath = getRelativeXPath(rule.getSourceField());
                String rawValue = xmlProcessor.evaluateXPath(lineElement, relativeXPath);
                // Use the standardized method from the base class
                applyRuleToField(line, rule, rawValue);
            } catch (Exception e) {
                // Error handling is mostly within applyRuleToField, but log context here
                log.warn("Error applying rule {} to ORDER line: {}", rule.getName(), e.getMessage());
                // If applyRuleToField throws for a required field, the exception will propagate up
            }
        }
        // Set line number (example - adjust based on actual requirements)
        // String lineNumber = xmlProcessor.evaluateXPath(lineElement, "LineNumberXPath");
        // line.setLineNumber(lineNumber);
        // Or use index if no specific field exists:
        // line.setLineNumber(String.format("%03d", index + 1)); // Need index from processLineItems

        // Perform any final line validation
        validateOrderLine(line);
    }

    /**
     * Extracts the relative path from a full XPath, assuming the last segment is the field name.
     * Basic implementation - might need refinement based on actual XPath structures.
     */
    private String getRelativeXPath(String fullXPath) {
        if (fullXPath == null) return null;
        int lastSlash = fullXPath.lastIndexOf("/");
        if (lastSlash >= 0 && lastSlash < fullXPath.length() - 1) {
            return fullXPath.substring(lastSlash + 1);
        }
        return fullXPath; // Return full path if no slash or it's the last char
    }

    // --- Specific Validations (Keep if needed) ---

    private void validateOrderHeader(OrderHeader header) {
        if (header == null) {
            throw new ValidationException("Order Header cannot be null after processing rules.");
        }
        if (header.getClient() == null) {
            throw new ValidationException("Client must be specified for Order Header.");
        }
        // Add more ORDER header specific validations as needed
        log.debug("Order Header validation passed.");
    }


    private void validateOrderLine(OrderLine line) {
        if (line == null) {
            throw new ValidationException("Order Line cannot be null after processing rules.");
        }
        if (line.getOrderHeader() == null) {
            throw new ValidationException("Header must be specified for Order Line.");
        }
        if (line.getClient() == null) {
            throw new ValidationException("Client must be specified for Order Line.");
        }
		// Example: Check required fields not covered by applyRuleToField's required check
        // if (line.getLineNumber() == null || line.getLineNumber().trim().isEmpty()) {
        //     throw new ValidationException("Line number is missing for ASN Line.");
        // }
        // Add more ORDER line specific validations as needed
        log.debug("Order Line validation passed for line number (if available): {}", line.getLineNumber());
    }

    // Note: Methods like isHeaderRule, isLineRule, setFieldValue are removed as their
    // functionality is replaced by TargetLevel and applyRuleToField from the base class.
}

