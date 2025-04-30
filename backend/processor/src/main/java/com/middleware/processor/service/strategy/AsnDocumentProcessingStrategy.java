package com.middleware.processor.service.strategy;

import com.middleware.processor.exception.ValidationException;
import com.middleware.shared.model.AsnHeader;
import com.middleware.shared.model.AsnLine;
import com.middleware.shared.model.Interface;
import com.middleware.shared.model.Client;
import com.middleware.processor.service.interfaces.AsnService;
import com.middleware.processor.service.interfaces.XmlValidationService;
import com.middleware.processor.service.factory.AsnFactory;
import com.middleware.processor.service.interfaces.MappingRuleService;
import com.middleware.shared.model.MappingRule;
import com.middleware.processor.service.util.XmlProcessor;
import com.middleware.processor.service.util.TransformationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import com.middleware.shared.service.util.CircuitBreakerService;
import com.middleware.shared.repository.ClientRepository;
import com.middleware.shared.model.TargetLevel;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Optional;
import java.util.Objects;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Collections;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * Concrete strategy for processing ASN (Advanced Shipping Notice) documents.
 * Extends the BaseDocumentProcessingTemplate for common functionality.
 * Standardized transaction management with MANDATORY propagation.
 */
@Slf4j
@Service
public class AsnDocumentProcessingStrategy extends BaseDocumentProcessingTemplate {

    private static final String ASN_ROOT_ELEMENT = "ASN";

    @Autowired
    private AsnService asnService;

    @Autowired
    private AsnFactory asnFactory;

    @Autowired
    private XmlProcessor xmlProcessor;

    @Autowired
    private TransformationService transformationService;

    @Autowired
    private XmlValidationService xmlValidationService;

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
    @Transactional(propagation = Propagation.REQUIRED)
    protected Object processSpecificDocument(Document document, Interface interfaceEntity) throws Exception {
        log.debug("Processing ASN document for interface: {}", interfaceEntity.getName());
        
        try {
            // Validate XML content using the validation service
            xmlValidationService.validateXmlContent(document, interfaceEntity);
            
            // Process ASN header
            AsnHeader asnHeader = createAsnHeaderFromMappingRules(document, interfaceEntity);
            if (asnHeader != null) {
                // Save header with circuit breaker protection
                asnHeader = asnService.createAsnHeader(asnHeader);
                
                if (asnHeader != null && asnHeader.getId() != null) {
                    // Process ASN lines using the header ID
                    List<AsnLine> asnLines = createAsnLinesFromMappingRules(document, interfaceEntity, asnHeader);
                    if (!asnLines.isEmpty()) {
                        // Save lines with circuit breaker protection
                        asnLines = asnService.createAsnLines(asnLines);
                    }
                    
                    log.info("Successfully processed ASN document for interface: {}", interfaceEntity.getName());
                    return asnHeader;
                } else {
                    throw new ValidationException("Failed to create ASN Header - no ID generated");
                }
            }
            
            throw new ValidationException("Failed to create ASN Header - header is null");
        } catch (Exception e) {
            log.error("Error processing ASN document for interface {}: {}", interfaceEntity.getName(), e.getMessage(), e);
            throw new ValidationException("Failed to process ASN document: " + e.getMessage());
        }
    }

    @Override
    protected void performAdditionalValidations(Document document, Interface interfaceEntity) throws ValidationException {
        // No additional validations needed here - all XML validation is handled by XmlValidationService
    }

    private AsnHeader createHeaderFromRules(Client client, List<MappingRule> headerRules) {
        // Fetch a fresh Client instance to avoid session issues
        Client freshClient = clientRepository.findById(client.getId())
            .orElseThrow(() -> new ValidationException("Client not found with ID: " + client.getId()));
            
        AsnHeader header = asnFactory.createDefaultHeader(freshClient);
        
        // Apply default values from mapping rules
        if (headerRules != null) {
            for (MappingRule rule : headerRules) {
                if (rule.getDefaultValue() != null && !rule.getDefaultValue().isEmpty()) {
                    try {
                        Object defaultValue = transformationService.transformAndConvert(
                            rule.getDefaultValue(),
                            rule.getTransformation(),
                            getTargetFieldType(header, rule.getTargetField())
                        );
                        setEntityField(header, rule.getTargetField(), defaultValue);
                    } catch (Exception e) {
                        handleRuleProcessingError(rule, e);
                    }
                }
            }
        }
        
        return header;
    }

    private void applyHeaderMappingRules(Document document, AsnHeader header, List<MappingRule> headerRules) {
        if (headerRules == null || headerRules.isEmpty()) {
            throw new ValidationException("No header mapping rules found");
        }

        for (MappingRule rule : headerRules) {
            if (rule.getTargetLevel() == TargetLevel.HEADER) {  // Only process HEADER level rules
                try {
                    // Let XmlProcessor handle the XPath evaluation directly with the full path
                    String rawValue = xmlProcessor.evaluateXPath(document, rule.getSourceField());
                    processFieldWithRule(header, rule, rawValue);
                    log.debug("Applied header rule {}: value = {}", rule.getName(), rawValue);
                } catch (Exception e) {
                    handleRuleProcessingError(rule, e);
                }
            }
        }
    }

    private List<AsnLine> processAsnLines(Document document, List<MappingRule> lineRules, AsnHeader header) {
        List<AsnLine> lines = new ArrayList<>();
        
        try {
            if (lineRules.isEmpty()) {
                throw new ValidationException("No line mapping rules found");
            }
            
            // Get all line-level rules
            List<MappingRule> lineLevelRules = lineRules.stream()
                .filter(rule -> rule.getTargetLevel() == TargetLevel.LINE)
                .collect(Collectors.toList());
                
            // Get the first rule's path to find line nodes
            String lineNodePath = lineLevelRules.get(0).getSourceField();
            int lastSlashIndex = lineNodePath.lastIndexOf('/');
            if (lastSlashIndex > 0) {
                lineNodePath = lineNodePath.substring(0, lastSlashIndex);
            }
            log.debug("Using line node path: {}", lineNodePath);
            
            // Get all line nodes using XPath
            NodeList lineNodes = xmlProcessor.evaluateXPathForNodes(document, lineNodePath);
            log.debug("Found {} line nodes in document", lineNodes.getLength());
            
            for (int i = 0; i < lineNodes.getLength(); i++) {
                Element lineElement = (Element) lineNodes.item(i);
                AsnLine line = createLineFromRules(header, header.getClient(), lineRules);
                
                // Apply mapping rules to line
                for (MappingRule rule : lineLevelRules) {
                    try {
                        // Get the field name from the rule's source field
                        String fieldName = rule.getSourceField().substring(rule.getSourceField().lastIndexOf('/') + 1);
                        String rawValue = xmlProcessor.evaluateXPath(lineElement, fieldName);
                        processFieldWithRule(line, rule, rawValue);
                        log.debug("Applied rule {} to line {}: value = {}", rule.getName(), i + 1, rawValue);
                    } catch (Exception e) {
                        handleRuleProcessingError(rule, e);
                    }
                }
                
                validateAsnLine(line, lineRules);
                lines.add(line);
                log.debug("Created ASN line {} with number {}", i + 1, line.getLineNumber());
            }
            
            return lines;
        } catch (Exception e) {
            log.error("Error processing ASN lines: {}", e.getMessage(), e);
            throw new ValidationException("Failed to process ASN lines: " + e.getMessage());
        }
    }

    private String findLineNodePath(List<MappingRule> lineLevelRules) {
        if (lineLevelRules.isEmpty()) {
            throw new ValidationException("No line-level mapping rules found");
        }

        // Get all unique paths
        List<String> paths = lineLevelRules.stream()
            .map(MappingRule::getSourceField)
            .distinct()
            .collect(Collectors.toList());

        // Split paths into segments and find the common path
        List<List<String>> pathSegments = paths.stream()
            .map(path -> Arrays.asList(path.split("/(?=\\*\\[local-name\\(\\)='[^']*'\\])")))
            .collect(Collectors.toList());

        // Find the common path up to the line node
        List<String> commonPath = new ArrayList<>();
        int minLength = pathSegments.stream().mapToInt(List::size).min().orElse(0);

        // Find the line node level by looking at the paths
        int lineNodeLevel = -1;
        for (int i = 0; i < minLength; i++) {
            final int level = i;
            String segment = pathSegments.get(0).get(level);
            if (pathSegments.stream().allMatch(segments -> segments.get(level).equals(segment))) {
                // Check if this is the line node level (parent of the field nodes)
                boolean isLineNode = pathSegments.stream()
                    .map(segments -> segments.size() > level + 1 ? segments.get(level + 1) : "")
                    .distinct()
                    .count() > 1;
                if (isLineNode) {
                    lineNodeLevel = level;
                    commonPath.add(segment);
                    break;
                }
                commonPath.add(segment);
            } else {
                break;
            }
        }

        if (lineNodeLevel == -1) {
            throw new ValidationException("Could not determine line node level from mapping rules");
        }

        // Join the path segments with forward slashes and ensure proper XPath format
        String xpath = String.join("/", commonPath);
        if (!xpath.startsWith("/")) {
            xpath = "/" + xpath;
        }

        log.debug("Generated XPath for line nodes: {}", xpath);
        return xpath;
    }

    private String getRelativeFieldPath(String lineNodePath, String fullPath) {
        // Extract the field-specific part of the XPath by finding the first different segment
        String[] lineParts = lineNodePath.split("/");
        String[] fullParts = fullPath.split("/");
        
        StringBuilder relativePath = new StringBuilder();
        boolean foundDifference = false;
        
        for (int i = 0; i < fullParts.length; i++) {
            if (i >= lineParts.length || !fullParts[i].equals(lineParts[i])) {
                foundDifference = true;
            }
            if (foundDifference) {
                if (relativePath.length() > 0) {
                    relativePath.append("/");
                }
                relativePath.append(fullParts[i]);
            }
        }
        
        String result = relativePath.toString();
        if (result.startsWith("/")) {
            result = result.substring(1);
        }
        
        return result;
    }

    private AsnLine createLineFromRules(AsnHeader header, Client client, List<MappingRule> lineRules) {
        AsnLine line = asnFactory.createDefaultLine(header, client);
        
        // Apply default values from mapping rules
        for (MappingRule rule : lineRules) {
            if (rule.getDefaultValue() != null && !rule.getDefaultValue().isEmpty()) {
                try {
                    Object defaultValue = transformationService.transformAndConvert(
                        rule.getDefaultValue(),
                        rule.getTransformation(),
                        getTargetFieldType(line, rule.getTargetField())
                    );
                    setEntityField(line, rule.getTargetField(), defaultValue);
                } catch (Exception e) {
                    handleRuleProcessingError(rule, e);
                }
            }
        }
        
        return line;
    }

    private void validateAsnLine(AsnLine line, List<MappingRule> lineRules) {
        // Check required fields based on mapping rules
        for (MappingRule rule : lineRules) {
            if (rule.getRequired()) {
                String fieldName = rule.getTargetField();
                try {
                    Object value = getEntityField(line, fieldName);
                    if (value == null || (value instanceof String && ((String) value).trim().isEmpty())) {
                        throw new ValidationException("Required field is missing or empty: " + fieldName);
                    }
                } catch (Exception e) {
                    throw new ValidationException("Failed to validate required field: " + fieldName);
                }
            }
        }
    }

    private Object getEntityField(Object entity, String fieldName) {
        try {
            // Convert database field name (snake_case) to Java field name (camelCase)
            StringBuilder javaFieldName = new StringBuilder(fieldName.toLowerCase());
            int pos;
            while ((pos = javaFieldName.indexOf("_")) != -1 && pos + 1 < javaFieldName.length()) {
                javaFieldName.deleteCharAt(pos);
                javaFieldName.setCharAt(pos, Character.toUpperCase(javaFieldName.charAt(pos)));
            }

            java.lang.reflect.Field field = entity.getClass().getDeclaredField(javaFieldName.toString());
            field.setAccessible(true);
            return field.get(entity);
        } catch (Exception e) {
            throw new ValidationException("Failed to get field value: " + fieldName, e);
        }
    }

    private void processFieldWithRule(Object entity, MappingRule rule, String rawValue) {
        try {
            Class<?> targetType = getTargetFieldType(entity, rule.getTargetField());
            boolean isNullable = isFieldNullable(entity, rule.getTargetField());
            
            // If field is mapped (rule exists)
            if (rawValue != null && !rawValue.isEmpty()) {
                // Use TransformationService to handle both transformation and type conversion
                Object transformedValue = transformationService.transformAndConvert(
                    rawValue,
                    rule.getTransformation(),
                    targetType
                );
                setEntityField(entity, rule.getTargetField(), transformedValue);
                log.debug("Set {} to transformed value: {}", rule.getTargetField(), transformedValue);
            } else if (!isNullable) {
                // For non-nullable fields, use factory default
                // Factory default will already be set from createDefaultHeader/createDefaultLine
                log.debug("Using factory default for non-nullable field {}", rule.getTargetField());
            } else {
                // For nullable fields, leave as null
                log.debug("Leaving nullable field {} as null", rule.getTargetField());
            }
        } catch (Exception e) {
            handleRuleProcessingError(rule, e);
        }
    }

    private boolean isFieldNullable(Object entity, String fieldName) {
        try {
            // Convert database field name (snake_case) to Java field name (camelCase)
            StringBuilder javaFieldName = new StringBuilder(fieldName.toLowerCase());
            int pos;
            while ((pos = javaFieldName.indexOf("_")) != -1 && pos + 1 < javaFieldName.length()) {
                javaFieldName.deleteCharAt(pos);
                javaFieldName.setCharAt(pos, Character.toUpperCase(javaFieldName.charAt(pos)));
            }

            Field field = entity.getClass().getDeclaredField(javaFieldName.toString());
            
            // Check for @Column(nullable = false) or @NotNull
            if (field.isAnnotationPresent(Column.class)) {
                Column column = field.getAnnotation(Column.class);
                if (!column.nullable()) {
                    return false;
                }
            }
            
            return !field.isAnnotationPresent(NotNull.class);
        } catch (Exception e) {
            log.warn("Could not determine nullability for field {}, assuming nullable", fieldName);
            return true;
        }
    }

    private void handleRuleProcessingError(MappingRule rule, Exception e) {
        String message = String.format("Failed to process mapping rule %s: %s", rule.getName(), e.getMessage());
        log.warn(message);
        if (rule.getRequired()) {
            throw new ValidationException(message, e);
        }
    }

    private void validateRequiredMappings(Document document, List<MappingRule> rules) {
        List<String> missingRequiredFields = rules.stream()
            .filter(MappingRule::getRequired)
            .filter(rule -> {
                try {
                    String value = xmlProcessor.evaluateXPath(document, rule.getSourceField());
                    return value == null || value.trim().isEmpty();
                } catch (Exception e) {
                    return true;
                }
            })
            .map(MappingRule::getTargetField)
            .collect(Collectors.toList());

        if (!missingRequiredFields.isEmpty()) {
            throw new ValidationException("Missing required fields: " + String.join(", ", missingRequiredFields));
        }
    }

    private Class<?> getTargetFieldType(Object entity, String fieldName) {
        try {
            // Convert database field name (snake_case) to Java field name (camelCase)
            StringBuilder javaFieldName = new StringBuilder(fieldName.toLowerCase());
            int pos;
            while ((pos = javaFieldName.indexOf("_")) != -1 && pos + 1 < javaFieldName.length()) {
                javaFieldName.deleteCharAt(pos);
                javaFieldName.setCharAt(pos, Character.toUpperCase(javaFieldName.charAt(pos)));
            }

            return entity.getClass().getDeclaredField(javaFieldName.toString()).getType();
        } catch (Exception e) {
            log.warn("Could not determine field type for {}, defaulting to String", fieldName);
            return String.class;
        }
    }

    private void setEntityField(Object entity, String fieldName, Object value) {
        try {
            // Convert database field name (snake_case) to Java field name (camelCase)
            StringBuilder javaFieldName = new StringBuilder(fieldName.toLowerCase());
            int pos;
            while ((pos = javaFieldName.indexOf("_")) != -1 && pos + 1 < javaFieldName.length()) {
                javaFieldName.deleteCharAt(pos);
                javaFieldName.setCharAt(pos, Character.toUpperCase(javaFieldName.charAt(pos)));
            }

            // Get the field and make it accessible
            java.lang.reflect.Field field = entity.getClass().getDeclaredField(javaFieldName.toString());
            field.setAccessible(true);

            // Set the value directly as it's already converted by TransformationService
            field.set(entity, value);
        } catch (Exception e) {
            log.warn("Failed to set field {} to value {}: {}", fieldName, value, e.getMessage());
            throw new ValidationException("Failed to set field " + fieldName + ": " + e.getMessage());
        }
    }

    @Override
    public String getName() {
        return "ASN Processing Strategy";
    }

    @Override
    public int getPriority() {
        return 10;
    }

    private List<MappingRule> getHeaderMappingRules(Long interfaceId) {
        return getActiveMappingRules(interfaceId).stream()
            .filter(this::isHeaderRule)
            .collect(Collectors.toList());
    }

    private List<MappingRule> getLineMappingRules(Long interfaceId) {
        return getActiveMappingRules(interfaceId).stream()
            .filter(this::isLineRule)
            .collect(Collectors.toList());
    }

    private boolean isHeaderRule(MappingRule rule) {
        return rule.getTargetLevel() == TargetLevel.HEADER;
    }

    private boolean isLineRule(MappingRule rule) {
        return rule.getTargetLevel() == TargetLevel.LINE;
    }

    private AsnHeader createAsnHeaderFromMappingRules(Document document, Interface interfaceEntity) {
        List<MappingRule> headerRules = getHeaderMappingRules(interfaceEntity.getId());
        if (headerRules.isEmpty()) {
            throw new ValidationException("No header mapping rules found for interface: " + interfaceEntity.getName());
        }
        
        AsnHeader header = createHeaderFromRules(interfaceEntity.getClient(), headerRules);
        applyHeaderMappingRules(document, header, headerRules);
        validateRequiredMappings(document, headerRules);
        return header;
    }

    private List<AsnLine> createAsnLinesFromMappingRules(Document document, Interface interfaceEntity, AsnHeader header) {
        List<MappingRule> lineRules = getLineMappingRules(interfaceEntity.getId());
        if (lineRules.isEmpty()) {
            log.warn("No line mapping rules found for interface: {}", interfaceEntity.getName());
            return Collections.emptyList();
        }
        
        List<AsnLine> lines = processAsnLines(document, lineRules, header);
        if (!lines.isEmpty()) {
            log.info("Created {} ASN lines", lines.size());
            return asnService.createAsnLines(lines);
        }
        
        return Collections.emptyList();
    }
}

