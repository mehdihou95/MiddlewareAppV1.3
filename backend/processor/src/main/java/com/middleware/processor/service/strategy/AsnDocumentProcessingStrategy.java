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

    @Override
    public boolean canHandle(String rootElement) {
        return ASN_ROOT_ELEMENT.equals(rootElement);
    }

    @Override
    public String getDocumentType() {
        return ASN_ROOT_ELEMENT;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
                
                // Process ASN lines
                List<AsnLine> asnLines = createAsnLinesFromMappingRules(document, interfaceEntity, asnHeader);
                if (!asnLines.isEmpty()) {
                    // Save lines with circuit breaker protection
                    asnLines = asnService.createAsnLines(asnLines);
                    
                    // Update header with lines
                    asnHeader.setLines(new HashSet<>(asnLines));
                    asnService.updateAsnHeader(asnHeader.getId(), asnHeader);
                }
            }
            
            log.info("Successfully processed ASN document for interface: {}", interfaceEntity.getName());
            return asnHeader;
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
        AsnHeader header = asnFactory.createDefaultHeader(client);
        
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
            try {
                String rawValue = xmlProcessor.evaluateXPath(document, rule.getSourceField());
                processFieldWithRule(header, rule, rawValue);
            } catch (Exception e) {
                handleRuleProcessingError(rule, e);
            }
        }
    }

    private List<AsnLine> processAsnLines(Document document, List<MappingRule> lineRules, AsnHeader header) {
        List<AsnLine> lines = new ArrayList<>();
        NodeList lineNodes = document.getElementsByTagName("ASN_LINE");
        
        for (int i = 0; i < lineNodes.getLength(); i++) {
            Element lineElement = (Element) lineNodes.item(i);
            AsnLine line = createLineFromRules(header, header.getClient(), lineRules);
            
            // Apply mapping rules to line
            for (MappingRule rule : lineRules) {
                try {
                    String rawValue = xmlProcessor.evaluateXPath(lineElement, rule.getSourceField());
                    processFieldWithRule(line, rule, rawValue);
                } catch (Exception e) {
                    handleRuleProcessingError(rule, e);
                }
            }
            
            validateAsnLine(line, lineRules);
            lines.add(line);
        }
        
        return lines;
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

    private String determineLineNodePath(List<MappingRule> lineRules, Interface interfaceEntity) {
        // Try to get the common parent path from the rules
        Optional<String> commonPath = findCommonXmlPath(lineRules);
        if (commonPath.isPresent()) {
            return commonPath.get();
        }

        // Use the first rule's source field as a fallback
        return lineRules.stream()
            .map(MappingRule::getSourceField)
            .filter(Objects::nonNull)
            .findFirst()
            .orElseThrow(() -> new ValidationException("No valid line node path found in mapping rules"));
    }

    private Optional<String> findCommonXmlPath(List<MappingRule> rules) {
        return rules.stream()
            .map(MappingRule::getSourceField)
            .filter(Objects::nonNull)
            .findFirst()
            .map(path -> {
                // Extract the parent path by removing the last segment
                int lastSlashIndex = path.lastIndexOf('/');
                if (lastSlashIndex > 0) {
                    String parentPath = path.substring(0, lastSlashIndex);
                    // If the parent path ends with a predicate, remove it
                    int predicateIndex = parentPath.indexOf('[');
                    if (predicateIndex > 0) {
                        parentPath = parentPath.substring(0, predicateIndex);
                    }
                    return parentPath;
                }
                return path;
            });
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
                // Use XML value
                Object transformedValue = transformationService.transformAndConvert(
                    rawValue,
                    rule.getTransformation(),
                    targetType
                );
                setEntityField(entity, rule.getTargetField(), transformedValue);
                log.debug("Set {} to XML value: {}", rule.getTargetField(), transformedValue);
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

    private void validateRequiredMappings(Map<String, List<MappingRule>> rulesByTable) {
        for (Map.Entry<String, List<MappingRule>> entry : rulesByTable.entrySet()) {
            List<MappingRule> missingRequired = entry.getValue().stream()
                .filter(MappingRule::getRequired)
                .filter(rule -> rule.getSourceField() == null || rule.getSourceField().isEmpty())
                .toList();
            
            if (!missingRequired.isEmpty()) {
                String missingFields = missingRequired.stream()
                    .map(MappingRule::getName)
                    .collect(java.util.stream.Collectors.joining(", "));
                throw new ValidationException(
                    String.format("Missing required mappings for table %s: %s", 
                        entry.getKey(), missingFields)
                );
            }
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

            // Set the value with appropriate type conversion
            if (value != null) {
                Class<?> fieldType = field.getType();
                Object convertedValue = value;
                
                if (fieldType == Integer.class || fieldType == int.class) {
                    convertedValue = Integer.valueOf(value.toString());
                } else if (fieldType == Long.class || fieldType == long.class) {
                    convertedValue = Long.valueOf(value.toString());
                } else if (fieldType == Double.class || fieldType == double.class) {
                    convertedValue = Double.valueOf(value.toString());
                } else if (fieldType == Boolean.class || fieldType == boolean.class) {
                    convertedValue = Boolean.valueOf(value.toString());
                }
                
                field.set(entity, convertedValue);
            }
        } catch (Exception e) {
            log.warn("Failed to set field {} to value {}: {}", fieldName, value, e.getMessage());
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

    private List<MappingRule> getHeaderMappingRules(Interface interfaceEntity) {
        List<MappingRule> allRules = getActiveMappingRules(interfaceEntity.getId());
        if (allRules.isEmpty()) {
            throw new ValidationException("No active mapping rules found for interface: " + interfaceEntity.getName());
        }
        
        List<MappingRule> headerRules = allRules.stream()
            .filter(rule -> "ASN_HEADERS".equals(rule.getTableName()))
            .collect(Collectors.toList());
            
        if (headerRules.isEmpty()) {
            throw new ValidationException("No header mapping rules found for interface: " + interfaceEntity.getName());
        }
        
        return headerRules;
    }
    
    private List<MappingRule> getLineMappingRules(Interface interfaceEntity) {
        List<MappingRule> allRules = getActiveMappingRules(interfaceEntity.getId());
        return allRules.stream()
            .filter(rule -> "ASN_LINES".equals(rule.getTableName()))
            .collect(Collectors.toList());
    }

    private void validateAsnHeader(AsnHeader header, List<MappingRule> headerRules) throws ValidationException {
        // Check required fields based on mapping rules
        for (MappingRule rule : headerRules) {
            if (rule.getRequired()) {
                String fieldName = rule.getTargetField();
                try {
                    Object value = getEntityField(header, fieldName);
                    if (value == null || (value instanceof String && ((String) value).trim().isEmpty())) {
                        throw new ValidationException("Required header field is missing or empty: " + fieldName);
                    }
                } catch (Exception e) {
                    throw new ValidationException("Failed to validate required header field: " + fieldName);
                }
            }
        }
        
        // Validate business rules
        if (header.getClient() == null) {
            throw new ValidationException("ASN header must have a client");
        }
        
        if (header.getAsnNumber() == null || header.getAsnNumber().trim().isEmpty()) {
            throw new ValidationException("ASN number is required");
        }
    }

    private AsnHeader createAsnHeaderFromMappingRules(Document document, Interface interfaceEntity) {
        List<MappingRule> headerRules = getHeaderMappingRules(interfaceEntity);
        if (headerRules.isEmpty()) {
            throw new ValidationException("No header mapping rules found for interface: " + interfaceEntity.getName());
        }
        
        AsnHeader header = createHeaderFromRules(interfaceEntity.getClient(), headerRules);
        applyHeaderMappingRules(document, header, headerRules);
        validateAsnHeader(header, headerRules);
        return header;
    }

    private List<AsnLine> createAsnLinesFromMappingRules(Document document, Interface interfaceEntity, AsnHeader header) {
        List<MappingRule> lineRules = getLineMappingRules(interfaceEntity);
        if (lineRules.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<AsnLine> lines = processAsnLines(document, lineRules, header);
        lines.forEach(line -> validateAsnLine(line, lineRules));
        return lines;
    }
}

