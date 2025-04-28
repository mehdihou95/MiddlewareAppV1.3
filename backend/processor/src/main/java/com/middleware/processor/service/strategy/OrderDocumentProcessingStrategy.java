package com.middleware.processor.service.strategy;

import com.middleware.processor.exception.ValidationException;
import com.middleware.shared.model.Interface;
import com.middleware.shared.model.ProcessedFile;
import com.middleware.shared.model.OrderHeader;
import com.middleware.shared.model.OrderLine;
import com.middleware.shared.model.Client;
import com.middleware.shared.model.MappingRule;
import com.middleware.processor.service.util.XmlProcessor;
import com.middleware.processor.service.util.TransformationService;
import com.middleware.processor.service.interfaces.OrderService;
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
import com.middleware.processor.service.factory.OrderFactory;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.time.LocalDateTime;
import org.springframework.web.multipart.MultipartFile;

/**
 * Implementation of the Order document processing strategy that handles XML documents
 * with different namespace prefix styles.
 */
@Component
public class OrderDocumentProcessingStrategy extends BaseDocumentProcessingTemplate {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderDocumentProcessingStrategy.class);
    private static final String ORDER_TYPE = "ORDER";
    
    @Autowired
    private com.middleware.processor.service.interfaces.OrderService orderService;
    
    @Autowired
    private TransformationService transformationService;
    
    @Autowired
    private OrderFactory orderFactory;
    
    @Override
    public String getDocumentType() {
        return "ORDER";
    }
    
    @Override
    public boolean canHandle(String documentType) {
        return "ORDER".equalsIgnoreCase(documentType);
    }
    
    @Override
    public String getName() {
        return "Order Document Processing Strategy";
    }

    @Override
    protected String getHeaderTableName() {
        return "ORDER_HEADERS";
    }

    @Override
    protected String getLineTableName() {
        return "ORDER_LINES";
    }

    @Override
    protected Object createDefaultHeader(Client client) {
        return orderFactory.createDefaultHeader(client);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    protected boolean processHeader(Object header, List<MappingRule> rules, Document document, 
            AtomicBoolean hasErrors, StringBuilder errorMessages) {
        OrderHeader orderHeader = (OrderHeader) header;
        
        for (MappingRule rule : rules) {
            if (rule.getIsActive()) {
                try {
                    String xmlPath = getXmlPath(rule);
                    if (xmlPath != null && !xmlPath.isEmpty()) {
                        String value = xmlProcessor.evaluateXPath(document, xmlPath);
                        if (value != null) {
                            setFieldValue(orderHeader, rule, value);
                        }
                    }
                } catch (Exception e) {
                    hasErrors.set(true);
                    errorMessages.append("Header mapping error for ").append(getXmlPath(rule))
                        .append(": ").append(e.getMessage()).append("; ");
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    protected Object saveHeader(Object header, AtomicBoolean hasErrors, StringBuilder errorMessages) {
        try {
            return circuitBreakerService.executeRepositoryOperation(
                () -> orderService.createOrderHeader((OrderHeader) header),
                () -> {
                    hasErrors.set(true);
                    errorMessages.append("Failed to save Order header: Circuit breaker open; ");
                    return header;
                }
            );
        } catch (Exception e) {
            hasErrors.set(true);
            errorMessages.append("Failed to save Order header: ").append(e.getMessage()).append("; ");
            logger.error("Error saving Order header: {}", e.getMessage(), e);
            return header;
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    protected boolean processLines(Object savedHeader, List<MappingRule> rules, Document document,
            Interface interfaceEntity, AtomicBoolean hasErrors, StringBuilder errorMessages) {
        OrderHeader header = (OrderHeader) savedHeader;
        List<OrderLine> lines = new ArrayList<>();
        
        // Group line rules by their parent paths
        Map<String, List<MappingRule>> lineRulesByParent = groupRulesByParentPath(rules);
        
        // Process each group of line rules
        Set<Node> processedNodes = new HashSet<>();
        for (Map.Entry<String, List<MappingRule>> entry : lineRulesByParent.entrySet()) {
            String parentPath = entry.getKey();
            List<MappingRule> rulesForPath = entry.getValue();
            
            NodeList lineNodes = findLineNodes(document, parentPath);
            
            if (lineNodes != null && lineNodes.getLength() > 0) {
                for (int i = 0; i < lineNodes.getLength(); i++) {
                    Node lineContext = lineNodes.item(i);
                    
                    if (!processedNodes.contains(lineContext)) {
                        processedNodes.add(lineContext);
                        
                        OrderLine line = orderFactory.createDefaultLine(header, interfaceEntity.getClient());
                        boolean hasLineError = false;
                        
                        for (MappingRule rule : rulesForPath) {
                            try {
                                String xmlPath = getXmlPath(rule);
                                if (xmlPath != null && !xmlPath.isEmpty()) {
                                    String relativePath = xmlProcessor.getRelativePath(xmlPath, parentPath);
                                    String value = xmlProcessor.evaluateXPath(lineContext, relativePath);
                                    
                                    if (value != null) {
                                        setFieldValue(line, rule, value);
                                    }
                                }
                            } catch (Exception e) {
                                logger.error("Error applying line mapping rule {} for field {}: {}", 
                                    getXmlPath(rule), getDatabaseField(rule), e.getMessage());
                                hasErrors.set(true);
                                hasLineError = true;
                                errorMessages.append("Line ").append(i + 1).append(" mapping error for ")
                                    .append(getXmlPath(rule)).append(": ").append(e.getMessage()).append("; ");
                                break;
                            }
                        }
                        if (!hasLineError) {
                            lines.add(line);
                        }
                    }
                }
            }
        }
        
        // Save lines if no errors
        if (!hasErrors.get() && !lines.isEmpty()) {
            try {
                circuitBreakerService.executeRepositoryOperation(
                    () -> {
                        orderService.createOrderLines(lines);
                        return null;
                    },
                    () -> {
                        hasErrors.set(true);
                        errorMessages.append("Failed to save Order lines: Circuit breaker open; ");
                        return null;
                    }
                );
            } catch (Exception e) {
                hasErrors.set(true);
                errorMessages.append("Failed to save Order lines: ").append(e.getMessage()).append("; ");
                logger.error("Error saving Order lines: {}", e.getMessage(), e);
            }
        }
        
        return !hasErrors.get();
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
        return 100; // High priority for Order processing
    }
}
