package com.middleware.processor.service.strategy;

import com.middleware.processor.exception.ValidationException;
import com.middleware.shared.model.Interface;
import com.middleware.shared.model.ProcessedFile;
import com.middleware.shared.model.AsnHeader;
import com.middleware.shared.model.AsnLine;
import com.middleware.shared.model.Client;
import com.middleware.shared.model.MappingRule;
import com.middleware.processor.service.util.XmlProcessor;
import com.middleware.processor.service.util.TransformationService;
import com.middleware.processor.service.interfaces.AsnService;
import com.middleware.processor.service.interfaces.XmlValidationService;
import com.middleware.shared.service.util.CircuitBreakerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.middleware.processor.service.factory.AsnFactory;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.time.LocalDateTime;
import org.springframework.web.multipart.MultipartFile;

/**
 * Implementation of the ASN document processing strategy that handles XML documents
 * with different namespace prefix styles.
 */
@Component
public class AsnDocumentProcessingStrategy extends BaseDocumentProcessingTemplate {
    
    private static final Logger logger = LoggerFactory.getLogger(AsnDocumentProcessingStrategy.class);
    private static final String ASN_TYPE = "ASN";
    
    @Autowired
    private com.middleware.processor.service.interfaces.AsnService asnService;
    
    @Autowired
    private TransformationService transformationService;
    
    @Autowired
    private AsnFactory asnFactory;
    
    @Autowired
    private XmlValidationService xmlValidationService;
    
    @Override
    public String getDocumentType() {
        return ASN_TYPE;
    }
    
    @Override
    public boolean canHandle(String documentType) {
        return ASN_TYPE.equalsIgnoreCase(documentType);
    }
    
    @Override
    public String getName() {
        return "ASN Document Processing Strategy";
    }

    @Override
    protected String getHeaderTableName() {
        return "ASN_HEADERS";
    }

    @Override
    protected String getLineTableName() {
        return "ASN_LINES";
    }

    @Override
    protected Object createDefaultHeader(Client client) {
        return asnFactory.createDefaultHeader(client);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    protected boolean processHeader(Object header, List<MappingRule> rules, Document document, 
            AtomicBoolean hasErrors, StringBuilder errorMessages) {
        logger.info("Starting ASN header processing");
        AsnHeader asnHeader = (AsnHeader) header;
        
        // Validate XML structure for header
        logger.info("Validating XML structure for header");
        if (!xmlValidationService.validateXmlStructure(document)) {
            String error = "Invalid XML structure for ASN header: " + xmlValidationService.getValidationErrorMessage();
            logger.error(error);
            hasErrors.set(true);
            errorMessages.append(error);
            return false;
        }
        logger.info("XML structure validation passed for header");
        
        for (MappingRule rule : rules) {
            if (rule.getIsActive()) {
                try {
                    String xmlPath = getXmlPath(rule);
                    if (xmlPath != null && !xmlPath.isEmpty()) {
                        logger.debug("Processing header mapping rule: {}", xmlPath);
                        String value = xmlProcessor.evaluateXPath(document, xmlPath);
                        if (value != null) {
                            setFieldValue(asnHeader, rule, value);
                            logger.debug("Set header field {} to value: {}", getDatabaseField(rule), value);
                        }
                    }
                } catch (Exception e) {
                    String error = "Header mapping error for " + getXmlPath(rule) + ": " + e.getMessage();
                    logger.error(error);
                    hasErrors.set(true);
                    errorMessages.append(error).append("; ");
                    return false;
                }
            }
        }
        logger.info("ASN header processing completed successfully");
        return true;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    protected Object saveHeader(Object header, AtomicBoolean hasErrors, StringBuilder errorMessages) {
        logger.info("Saving ASN header");
        try {
            AsnHeader asnHeader = (AsnHeader) header;
            return circuitBreakerService.executeRepositoryOperation(
                () -> {
                    AsnHeader savedHeader = asnService.createAsnHeader(asnHeader);
                    logger.info("ASN header saved with id: {}", savedHeader.getId());
                    return savedHeader;
                },
                () -> {
                    String error = "Failed to save ASN header: Circuit breaker open";
                    logger.error(error);
                    hasErrors.set(true);
                    errorMessages.append(error).append("; ");
                    return header;
                }
            );
        } catch (Exception e) {
            String error = "Failed to save ASN header: " + e.getMessage();
            logger.error(error);
            hasErrors.set(true);
            errorMessages.append(error).append("; ");
            return header;
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    protected boolean processLines(Object savedHeader, List<MappingRule> rules, Document document,
            Interface interfaceEntity, AtomicBoolean hasErrors, StringBuilder errorMessages) {
        logger.info("Starting ASN line processing");
        AsnHeader header = (AsnHeader) savedHeader;
        List<AsnLine> lines = new ArrayList<>();
        
        // Validate XML structure for lines
        logger.info("Validating XML structure for lines");
        if (!xmlValidationService.validateXmlStructure(document)) {
            String error = "Invalid XML structure for ASN lines: " + xmlValidationService.getValidationErrorMessage();
            logger.error(error);
            hasErrors.set(true);
            errorMessages.append(error);
            return false;
        }
        logger.info("XML structure validation passed for lines");
        
        // Group line rules by their parent paths
        Map<String, List<MappingRule>> lineRulesByParent = groupRulesByParentPath(rules);
        logger.info("Grouped {} line rules by parent paths", lineRulesByParent.size());
        
        // Process each group of line rules
        Set<Node> processedNodes = new HashSet<>();
        for (Map.Entry<String, List<MappingRule>> entry : lineRulesByParent.entrySet()) {
            String parentPath = entry.getKey();
            List<MappingRule> rulesForPath = entry.getValue();
            
            logger.info("Processing line rules for parent path: {}", parentPath);
            NodeList lineNodes = findLineNodes(document, parentPath);
            
            if (lineNodes != null && lineNodes.getLength() > 0) {
                logger.info("Found {} line nodes for path: {}", lineNodes.getLength(), parentPath);
                for (int i = 0; i < lineNodes.getLength(); i++) {
                    Node lineContext = lineNodes.item(i);
                    
                    if (!processedNodes.contains(lineContext)) {
                        processedNodes.add(lineContext);
                        
                        AsnLine line = asnFactory.createDefaultLine(header, interfaceEntity.getClient());
                        boolean hasLineError = false;
                        
                        for (MappingRule rule : rulesForPath) {
                            try {
                                String xmlPath = getXmlPath(rule);
                                if (xmlPath != null && !xmlPath.isEmpty()) {
                                    String relativePath = xmlProcessor.getRelativePath(xmlPath, parentPath);
                                    String value = xmlProcessor.evaluateXPath(lineContext, relativePath);
                                    
                                    if (value != null) {
                                        setFieldValue(line, rule, value);
                                        logger.debug("Set line field {} to value: {}", getDatabaseField(rule), value);
                                    }
                                }
                            } catch (Exception e) {
                                String error = "Line " + (i + 1) + " mapping error for " + getXmlPath(rule) + ": " + e.getMessage();
                                logger.error(error);
                                hasErrors.set(true);
                                hasLineError = true;
                                errorMessages.append(error).append("; ");
                                break;
                            }
                        }
                        if (!hasLineError) {
                            lines.add(line);
                            logger.debug("Added line {} to processing batch", i + 1);
                        }
                    }
                }
            }
        }
        
        // Save lines if no errors
        if (!hasErrors.get() && !lines.isEmpty()) {
            logger.info("Saving {} ASN lines", lines.size());
            try {
                circuitBreakerService.executeRepositoryOperation(
                    () -> {
                        asnService.createAsnLines(lines);
                        logger.info("Successfully saved {} ASN lines", lines.size());
                        return null;
                    },
                    () -> {
                        String error = "Failed to save ASN lines: Circuit breaker open";
                        logger.error(error);
                        hasErrors.set(true);
                        errorMessages.append(error).append("; ");
                        return null;
                    }
                );
            } catch (Exception e) {
                String error = "Failed to save ASN lines: " + e.getMessage();
                logger.error(error);
                hasErrors.set(true);
                errorMessages.append(error).append("; ");
            }
        }
        
        boolean success = !hasErrors.get();
        logger.info("ASN line processing completed with status: {}", success ? "SUCCESS" : "FAILED");
        return success;
    }

    /**
     * Group mapping rules by their parent XML paths.
     */
    private Map<String, List<MappingRule>> groupRulesByParentPath(List<MappingRule> rules) {
        Map<String, List<MappingRule>> rulesByParent = new HashMap<>();
        
        for (MappingRule rule : rules) {
            if (rule.getIsActive()) {
                String xmlPath = getXmlPath(rule);
                if (xmlPath != null && !xmlPath.isEmpty()) {
                    String parentPath = xmlProcessor.getParentPath(xmlPath);
                    rulesByParent.computeIfAbsent(parentPath, k -> new ArrayList<>()).add(rule);
                }
            }
        }
        
        return rulesByParent;
    }
    
    /**
     * Find line nodes in the document using multiple approaches.
     */
    private NodeList findLineNodes(Document document, String parentPath) {
        try {
            NodeList nodes = xmlProcessor.evaluateXPathNodeList(document, parentPath);
            if (nodes != null && nodes.getLength() > 0) {
                return nodes;
            }
            
            String[] commonPatterns = {
                "//Items/Item",
                "//tns:Items/tns:Item",
                "//*[local-name()='Items']/*[local-name()='Item']",
                "//Lines/Line",
                "//tns:Lines/tns:Line",
                "//*[local-name()='Lines']/*[local-name()='Line']",
                "//Details/Detail",
                "//tns:Details/tns:Detail",
                "//*[local-name()='Details']/*[local-name()='Detail']"
            };
            
            for (String pattern : commonPatterns) {
                nodes = xmlProcessor.evaluateXPathNodeList(document, pattern);
                if (nodes != null && nodes.getLength() > 0) {
                    logger.info("Found line nodes using pattern: {}", pattern);
                    return nodes;
                }
            }
            
            return findRepeatingElements(document);
            
        } catch (Exception e) {
            logger.error("Error finding line nodes: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Find repeating elements in the document that might be line items.
     */
    private NodeList findRepeatingElements(Document document) {
        try {
            NodeList allElements = document.getElementsByTagName("*");
            Map<String, List<Node>> elementsByName = new HashMap<>();
            
            for (int i = 0; i < allElements.getLength(); i++) {
                Node node = allElements.item(i);
                String name = node.getLocalName();
                if (name != null) {
                    elementsByName.computeIfAbsent(name, k -> new ArrayList<>()).add(node);
                }
            }
            
            for (Map.Entry<String, List<Node>> entry : elementsByName.entrySet()) {
                List<Node> nodes = entry.getValue();
                if (nodes.size() > 1) {
                    Node parent = nodes.get(0).getParentNode();
                    boolean sameParent = true;
                    
                    for (int i = 1; i < nodes.size(); i++) {
                        if (nodes.get(i).getParentNode() != parent) {
                            sameParent = false;
                            break;
                        }
                    }
                    
                    if (sameParent) {
                        return createNodeList(nodes);
                    }
                }
            }
            
            return null;
        } catch (Exception e) {
            logger.error("Error finding repeating elements: {}", e.getMessage());
            return null;
        }
    }
    
    private NodeList createNodeList(List<Node> nodes) {
        return new NodeList() {
            @Override
            public Node item(int index) {
                return nodes.get(index);
            }

            @Override
            public int getLength() {
                return nodes.size();
            }
        };
    }
    
    /**
     * Set a field value on an entity using reflection.
     */
    private void setFieldValue(Object entity, MappingRule rule, String value) throws Exception {
        if (value == null || value.isEmpty()) {
            return;
        }
        
        String fieldName = getDatabaseField(rule);
        if (fieldName == null || fieldName.isEmpty()) {
            return;
        }
        
        if (fieldName.contains("_")) {
            fieldName = xmlProcessor.toCamelCase(fieldName);
        }
        
        String methodName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        
        Method[] methods = entity.getClass().getMethods();
        Method setterMethod = null;
        
        for (Method method : methods) {
            if (method.getName().equalsIgnoreCase(methodName) && method.getParameterCount() == 1) {
                setterMethod = method;
                break;
            }
        }
        
        if (setterMethod == null) {
            throw new NoSuchMethodException("No setter method found for field: " + fieldName);
        }
        
        Class<?> paramType = setterMethod.getParameterTypes()[0];
        Object transformedValue = transformValue(value, paramType, rule.getTransformation());
        setterMethod.invoke(entity, transformedValue);
    }
    
    /**
     * Transform a value based on its target type and transformation rule.
     */
    private Object transformValue(String value, Class<?> targetType, String transformation) throws Exception {
        if (value == null || value.isEmpty()) {
            return null;
        }

        try {
            return transformationService.transformAndConvert(value, transformation, targetType);
        } catch (Exception e) {
            logger.error("Error transforming value '{}' to type '{}' with transformation '{}': {}", 
                value, targetType.getName(), transformation, e.getMessage());
            throw e;
        }
    }
    
    /**
     * Get the XML path from a mapping rule.
     */
    private String getXmlPath(MappingRule rule) {
        try {
            Method getSourceField = rule.getClass().getMethod("getSourceField");
            String sourceField = (String) getSourceField.invoke(rule);
            if (sourceField != null && !sourceField.isEmpty()) {
                return sourceField;
            }
        } catch (Exception e) {
            // Field doesn't exist, try the old field name
        }
        
        try {
            Method getXmlPath = rule.getClass().getMethod("getXmlPath");
            return (String) getXmlPath.invoke(rule);
        } catch (Exception e) {
            logger.error("Error getting XML path from rule: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Get the database field from a mapping rule.
     */
    private String getDatabaseField(MappingRule rule) {
        try {
            Method getTargetField = rule.getClass().getMethod("getTargetField");
            String targetField = (String) getTargetField.invoke(rule);
            if (targetField != null && !targetField.isEmpty()) {
                return targetField;
            }
        } catch (Exception e) {
            // Field doesn't exist, try the old field name
        }
        
        try {
            Method getDatabaseField = rule.getClass().getMethod("getDatabaseField");
            return (String) getDatabaseField.invoke(rule);
        } catch (Exception e) {
            logger.error("Error getting database field from rule: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public int getPriority() {
        return 100; // High priority for ASN processing
    }
}
