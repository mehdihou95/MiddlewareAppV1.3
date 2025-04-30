package com.middleware.processor.service.strategy;

import com.middleware.processor.exception.ValidationException;
import com.middleware.processor.service.factory.AsnFactory;
import com.middleware.processor.service.interfaces.AsnService;
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
 * Concrete strategy for processing ASN (Advanced Shipping Notice) documents.
 * Extends the BaseDocumentProcessingTemplate for common functionality.
 * Uses standardized field and line processing methods from the base class.
 */
@Slf4j
@Service
public class AsnDocumentProcessingStrategy extends BaseDocumentProcessingTemplate {

    private static final String ASN_ROOT_ELEMENT = "ASN";
    private static final String DEFAULT_ASN_LINE_XPATH = "//ASN_LINE"; // Default XPath if derivation fails

    @Autowired
    private AsnService asnService;

    @Autowired
    private AsnFactory asnFactory;

    @Autowired
    private ClientRepository clientRepository;

    @Override
    public boolean canHandle(String rootElement) {
        return ASN_ROOT_ELEMENT.equals(rootElement);
    }

    @Override
    public String getDocumentType() {
        return ASN_ROOT_ELEMENT;
    }

    @Override
    public String getName() {
        return "ASN";
    }

    /**
     * Processes the specific ASN document content.
     * Creates header and line entities based on mapping rules.
     * Runs within the transaction started by the base class processDocument method.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    protected Object processSpecificDocument(Document document, Interface interfaceEntity) throws Exception {
        log.debug("Processing ASN specific document for interface: {}", interfaceEntity.getName());

        // Fetch a fresh Client instance to avoid session issues
        Client freshClient = clientRepository.findById(interfaceEntity.getClient().getId())
            .orElseThrow(() -> new ValidationException("Client not found with ID: " + interfaceEntity.getClient().getId()));

        List<MappingRule> allRules = getActiveMappingRules(interfaceEntity.getId());

        // 1. Process Header
        AsnHeader asnHeader = createAndProcessHeader(document, freshClient, allRules);

        // 2. Process Lines using the generic line processing method
        List<AsnLine> asnLines = processLineItems(
            document,
            null, // Pass null header initially
            allRules,
            DEFAULT_ASN_LINE_XPATH,
            AsnHeader::getClient,
            (header, client) -> asnFactory.createDefaultLine(header, client),
            (line, element) -> applyAsnLineRules(line, element, allRules)
        );

        // 3. Batch save header and lines in a single transaction
        return saveAsnDocumentBatch(asnHeader, asnLines);
    }

    /**
     * Saves the ASN document (header and lines) in a batch operation
     */
    @Transactional(propagation = Propagation.REQUIRED)
    private AsnHeader saveAsnDocumentBatch(AsnHeader header, List<AsnLine> lines) {
        log.debug("Saving ASN document batch - Header and {} lines", lines.size());

        // 1. Save header first to get ID
        AsnHeader savedHeader = asnService.createAsnHeader(header);
        if (savedHeader == null || savedHeader.getId() == null) {
            throw new ValidationException("Failed to save ASN Header or generate ID.");
        }

        // 2. Update all lines with the saved header
        lines.forEach(line -> {
            line.setHeader(savedHeader);
            line.setClient(savedHeader.getClient());
        });

        // 3. Batch save all lines
        if (!lines.isEmpty()) {
            log.debug("Batch saving {} ASN lines", lines.size());
            asnService.createAsnLines(lines);
        }

        log.info("Successfully saved ASN document batch - Header ID: {}, Lines: {}", 
            savedHeader.getId(), lines.size());
        return savedHeader;
    }

    /**
     * Creates the AsnHeader entity and applies header-level mapping rules.
     */
    private AsnHeader createAndProcessHeader(Document document, Client client, List<MappingRule> allRules) throws Exception {
        AsnHeader header = asnFactory.createDefaultHeader(client);

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
        validateAsnHeader(header);

        return header;
    }

    /**
     * Applies mapping rules specific to an AsnLine entity within the context of its XML element.
     * This method is passed as a lambda to the base class processLineItems.
     */
    private void applyAsnLineRules(AsnLine line, Element lineElement, List<MappingRule> allRules) {
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
                log.warn("Error applying rule 	{}	 to ASN line: {}", rule.getName(), e.getMessage());
                // If applyRuleToField throws for a required field, the exception will propagate up
            }
        }
        // Set line number (example - adjust based on actual requirements)
        // String lineNumber = xmlProcessor.evaluateXPath(lineElement, "LineNumberXPath");
        // line.setLineNumber(lineNumber);

        // Perform any final line validation
        validateAsnLine(line);
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

    private void validateAsnHeader(AsnHeader header) {
        if (header == null) {
            throw new ValidationException("ASN Header cannot be null after processing rules.");
        }
        if (header.getClient() == null) {
            throw new ValidationException("Client must be specified for ASN Header.");
        }
        // Add more ASN header specific validations as needed
        log.debug("ASN Header validation passed.");
    }

    private void validateAsnLine(AsnLine line) {
        if (line == null) {
            throw new ValidationException("ASN Line cannot be null after processing rules.");
        }
        if (line.getHeader() == null) {
            throw new ValidationException("Header must be specified for ASN Line.");
        }
        if (line.getClient() == null) {
            throw new ValidationException("Client must be specified for ASN Line.");
        }
        // Example: Check required fields not covered by applyRuleToField's required check
        // if (line.getLineNumber() == null || line.getLineNumber().trim().isEmpty()) {
        //     throw new ValidationException("Line number is missing for ASN Line.");
        // }
        // Add more ASN line specific validations as needed
        log.debug("ASN Line validation passed for line number (if available): {}", line.getLineNumber());
    }

    // Note: Methods like getTargetFieldType, isFieldNullable, setEntityField, findField,
    // convertDbNameToCamelCase are now expected to be in BaseDocumentProcessingTemplate.
    // Remove them from here if they exist.
}

