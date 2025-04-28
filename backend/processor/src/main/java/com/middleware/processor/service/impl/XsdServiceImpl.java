package com.middleware.processor.service.impl;

import com.middleware.processor.exception.ValidationException;
import com.middleware.shared.model.Interface;
import com.middleware.shared.model.MappingRule;
import com.middleware.shared.repository.MappingRuleRepository;
import com.middleware.processor.service.interfaces.XsdService;
import com.middleware.processor.service.interfaces.InterfaceService;
import com.middleware.shared.service.util.CircuitBreakerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Enhanced implementation of XsdService.
 * Provides operations for validating and processing XSD files with support for all XML types.
 */
@Slf4j
@Service
public class XsdServiceImpl implements XsdService {

    @Autowired
    private MappingRuleRepository mappingRuleRepository;

    @Autowired
    private CircuitBreakerService circuitBreakerService;

    @Autowired
    private InterfaceService interfaceService;

    @Override
    public boolean validateXsdSchema(MultipartFile file) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setValidating(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.parse(file.getInputStream());
            return true;
        } catch (ParserConfigurationException | SAXException | IOException e) {
            log.error("XSD validation failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public Interface processXsdSchema(MultipartFile file, Interface interfaceEntity) {
        if (!validateXsdSchema(file)) {
            throw new ValidationException("Invalid XSD schema");
        }

        String rootElement = getRootElement(file);
        String namespace = getNamespace(file);

        interfaceEntity.setRootElement(rootElement);
        interfaceEntity.setNamespace(namespace);
        interfaceEntity.setSchemaPath(file.getOriginalFilename());

        return interfaceEntity;
    }

    @Override
    public String getRootElement(MultipartFile file) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(file.getInputStream());
            Element root = document.getDocumentElement();
            return root.getLocalName();
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new ValidationException("Failed to get root element from XSD schema", e);
        }
    }

    @Override
    public String getNamespace(MultipartFile file) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(file.getInputStream());
            Element root = document.getDocumentElement();
            return root.getNamespaceURI();
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new ValidationException("Failed to get namespace from XSD schema", e);
        }
    }

    @Override
    public List<Map<String, String>> getAllNamespaces(MultipartFile file) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(file.getInputStream());
            Element root = document.getDocumentElement();
            
            Map<String, String> namespaceMap = extractNamespaces(root);
            
            List<Map<String, String>> namespaces = new ArrayList<>();
            for (Map.Entry<String, String> entry : namespaceMap.entrySet()) {
                Map<String, String> namespace = new HashMap<>();
                namespace.put("prefix", entry.getKey());
                namespace.put("uri", entry.getValue());
                namespaces.add(namespace);
            }
            
            return namespaces;
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new ValidationException("Failed to get namespaces from XSD schema", e);
        }
    }

    /**
     * Extract all namespace declarations from an element and its descendants.
     */
    private Map<String, String> extractNamespaces(Element element) {
        Map<String, String> namespaces = new LinkedHashMap<>();
        
        // Process the current element's namespace
        String namespaceURI = element.getNamespaceURI();
        if (namespaceURI != null && !namespaceURI.isEmpty()) {
            String prefix = element.getPrefix();
            if (prefix == null || prefix.isEmpty()) {
                namespaces.put("_default_", namespaceURI);
            } else {
                namespaces.put(prefix, namespaceURI);
            }
        }
        
        // Process namespace declarations in attributes
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attr = attributes.item(i);
            String nodeName = attr.getNodeName();
            
            if (nodeName.equals("xmlns")) {
                // Default namespace
                namespaces.put("_default_", attr.getNodeValue());
            } else if (nodeName.startsWith("xmlns:")) {
                // Prefixed namespace
                String prefix = nodeName.substring(6); // after "xmlns:"
                namespaces.put(prefix, attr.getNodeValue());
            }
        }
        
        // Process child elements recursively
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                namespaces.putAll(extractNamespaces((Element) child));
            }
        }
        
        return namespaces;
    }

    @Override
    public List<Map<String, Object>> getXsdStructure(String xsdPath) {
        try {
            if (xsdPath == null || xsdPath.trim().isEmpty()) {
                throw new ValidationException("XSD path cannot be null or empty");
            }

            // Normalize path separators to forward slashes
            String normalizedPath = xsdPath.replace("\\", "/");
            
            // Handle relative path from resources directory
            String resourcePath = normalizedPath;
            if (normalizedPath.startsWith("backend/src/main/resources/")) {
                resourcePath = normalizedPath.substring("backend/src/main/resources/".length());
            }
            
            // Try loading from classpath first
            InputStream inputStream = null;
            try {
                ClassPathResource resource = new ClassPathResource(resourcePath);
                inputStream = resource.getInputStream();
            } catch (IOException e) {
                // If not found in classpath, try absolute path
                Path absolutePath = Paths.get(normalizedPath);
                if (Files.exists(absolutePath)) {
                    inputStream = Files.newInputStream(absolutePath);
                } else {
                    // Try one more time with just the filename from resources
                    try {
                        String fileName = Paths.get(resourcePath).getFileName().toString();
                        ClassPathResource resource = new ClassPathResource(fileName);
                        inputStream = resource.getInputStream();
                    } catch (IOException e2) {
                        throw new ValidationException(
                            String.format("XSD file not found. Tried:\n" +
                                        "- Classpath: %s\n" +
                                        "- Absolute path: %s\n" +
                                        "- Resources filename: %s",
                                        resourcePath, normalizedPath, Paths.get(resourcePath).getFileName())
                        );
                    }
                }
            }
            
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);
            Element root = document.getDocumentElement();
            
            // Extract all namespaces
            final Map<String, String> namespaceMap = extractNamespaces(root);
            
            // Create a namespace context for XPath evaluation
            NamespaceContext namespaceContext = new NamespaceContext() {
                @Override
                public String getNamespaceURI(String prefix) {
                    return namespaceMap.getOrDefault(prefix, XMLConstants.NULL_NS_URI);
                }
                
                @Override
                public String getPrefix(String namespaceURI) {
                    for (Map.Entry<String, String> entry : namespaceMap.entrySet()) {
                        if (entry.getValue().equals(namespaceURI)) {
                            return entry.getKey();
                        }
                    }
                    return null;
                }
                
                @Override
                public Iterator<String> getPrefixes(String namespaceURI) {
                    List<String> prefixes = new ArrayList<>();
                    for (Map.Entry<String, String> entry : namespaceMap.entrySet()) {
                        if (entry.getValue().equals(namespaceURI)) {
                            prefixes.add(entry.getKey());
                        }
                    }
                    return prefixes.iterator();
                }
            };
            
            List<Map<String, Object>> structure = new ArrayList<>();
            
            // Add schema info
            Map<String, Object> schemaInfo = new HashMap<>();
            schemaInfo.put("name", root.getLocalName());
            schemaInfo.put("type", "schema");
            schemaInfo.put("namespace", root.getNamespaceURI());
            
            // Add namespaces to schema info
            List<Map<String, String>> namespacesList = new ArrayList<>();
            for (Map.Entry<String, String> entry : namespaceMap.entrySet()) {
                Map<String, String> ns = new HashMap<>();
                ns.put("prefix", entry.getKey());
                ns.put("uri", entry.getValue());
                namespacesList.add(ns);
            }
            schemaInfo.put("namespaces", namespacesList);
            
            // Add any schema-level attributes
            NamedNodeMap attributes = root.getAttributes();
            if (attributes != null && attributes.getLength() > 0) {
                Map<String, String> schemaAttributes = new HashMap<>();
                for (int i = 0; i < attributes.getLength(); i++) {
                    Node attr = attributes.item(i);
                    if (!attr.getNodeName().startsWith("xmlns")) {
                        schemaAttributes.put(attr.getLocalName(), attr.getNodeValue());
                    }
                }
                schemaInfo.put("attributes", schemaAttributes);
            }
            structure.add(schemaInfo);
            
            // Process global complex types first
            Map<String, Map<String, Object>> complexTypes = processComplexTypes(document, root, namespaceContext);
            
            // Get all global element declarations
            NodeList elements = document.getElementsByTagNameNS(root.getNamespaceURI(), "element");
            for (int i = 0; i < elements.getLength(); i++) {
                Element element = (Element) elements.item(i);
                // Only process global elements (those that are direct children of schema)
                if (element.getParentNode().equals(root)) {
                    Map<String, Object> elementInfo = processElement(element, complexTypes, namespaceContext);
                    structure.add(elementInfo);
                }
            }
            
            return structure;
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new ValidationException("Failed to analyze XSD structure: " + e.getMessage(), e);
        }
    }

    /**
     * Process all complex types in the document and build a map of their structures.
     */
    private Map<String, Map<String, Object>> processComplexTypes(Document document, Element root, NamespaceContext namespaceContext) {
        Map<String, Map<String, Object>> complexTypes = new HashMap<>();
        
        NodeList complexTypeNodes = document.getElementsByTagNameNS(root.getNamespaceURI(), "complexType");
        for (int i = 0; i < complexTypeNodes.getLength(); i++) {
            Element complexType = (Element) complexTypeNodes.item(i);
            
            // Only process global complex types (those that are direct children of schema)
            if (complexType.getParentNode().equals(root)) {
                String name = complexType.getAttribute("name");
                if (name != null && !name.isEmpty()) {
                    Map<String, Object> typeInfo = new HashMap<>();
                    typeInfo.put("name", name);
                    typeInfo.put("type", "complexType");
                    
                    // Check for mixed content
                    String mixed = complexType.getAttribute("mixed");
                    if (mixed != null && !mixed.isEmpty()) {
                        typeInfo.put("mixed", Boolean.parseBoolean(mixed));
                    }
                    
                    // Process attributes
                    List<Map<String, Object>> attributes = processAttributes(complexType);
                    if (!attributes.isEmpty()) {
                        typeInfo.put("attributes", attributes);
                    }
                    
                    // Process child elements
                    List<Map<String, Object>> childElements = new ArrayList<>();
                    
                    // Process sequence, choice, or all elements
                    for (String compositor : new String[]{"sequence", "choice", "all"}) {
                        NodeList compositors = complexType.getElementsByTagNameNS(root.getNamespaceURI(), compositor);
                        if (compositors.getLength() > 0) {
                            Element compositorElement = (Element) compositors.item(0);
                            typeInfo.put("compositor", compositor);
                            
                            NodeList childElementNodes = compositorElement.getElementsByTagNameNS(root.getNamespaceURI(), "element");
                            for (int j = 0; j < childElementNodes.getLength(); j++) {
                                Element childElement = (Element) childElementNodes.item(j);
                                // Only process immediate children
                                if (childElement.getParentNode().equals(compositorElement)) {
                                    Map<String, Object> childInfo = processElement(childElement, complexTypes, namespaceContext);
                                    childElements.add(childInfo);
                                }
                            }
                            
                            break; // Only process the first compositor found
                        }
                    }
                    
                    if (!childElements.isEmpty()) {
                        typeInfo.put("elements", childElements);
                    }
                    
                    complexTypes.put(name, typeInfo);
                }
            }
        }
        
        return complexTypes;
    }

    /**
     * Process attributes of an element.
     */
    private List<Map<String, Object>> processAttributes(Element element) {
        List<Map<String, Object>> attributes = new ArrayList<>();
        
        NodeList attributeNodes = element.getElementsByTagNameNS(element.getNamespaceURI(), "attribute");
        for (int i = 0; i < attributeNodes.getLength(); i++) {
            Element attribute = (Element) attributeNodes.item(i);
            
            // Only process immediate children or children of attributeGroup
            Node parent = attribute.getParentNode();
            if (parent.equals(element) || parent.getLocalName().equals("attributeGroup")) {
                Map<String, Object> attrInfo = new HashMap<>();
                attrInfo.put("name", attribute.getAttribute("name"));
                
                String type = attribute.getAttribute("type");
                if (type != null && !type.isEmpty()) {
                    attrInfo.put("type", type);
                }
                
                String use = attribute.getAttribute("use");
                if (use != null && !use.isEmpty()) {
                    attrInfo.put("required", "required".equals(use));
                }
                
                String defaultValue = attribute.getAttribute("default");
                if (defaultValue != null && !defaultValue.isEmpty()) {
                    attrInfo.put("default", defaultValue);
                }
                
                attributes.add(attrInfo);
            }
        }
        
        return attributes;
    }

    /**
     * Process an element and its children recursively.
     */
    private Map<String, Object> processElement(Element element, Map<String, Map<String, Object>> complexTypes, NamespaceContext namespaceContext) {
        Map<String, Object> elementInfo = new HashMap<>();
        elementInfo.put("name", element.getAttribute("name"));
        
        // Add namespace information
        String namespaceURI = element.getNamespaceURI();
        if (namespaceURI != null && !namespaceURI.isEmpty()) {
            elementInfo.put("namespace", namespaceURI);
            String prefix = element.getPrefix();
            if (prefix != null && !prefix.isEmpty()) {
                elementInfo.put("prefix", prefix);
            }
        }
        
        // Handle type attribute
        String type = element.getAttribute("type");
        if (!type.isEmpty()) {
            elementInfo.put("type", type);
            
            // If this is a reference to a complex type, include its structure
            if (type.contains(":")) {
                // Handle qualified type names (with prefix)
                String localType = type.substring(type.indexOf(':') + 1);
                if (complexTypes.containsKey(localType)) {
                    Map<String, Object> typeInfo = complexTypes.get(localType);
                    
                    // Include child elements from the complex type
                    if (typeInfo.containsKey("elements")) {
                        elementInfo.put("elements", typeInfo.get("elements"));
                    }
                    
                    // Include attributes from the complex type
                    if (typeInfo.containsKey("attributes")) {
                        elementInfo.put("attributes", typeInfo.get("attributes"));
                    }
                    
                    // Include mixed content information
                    if (typeInfo.containsKey("mixed")) {
                        elementInfo.put("mixed", typeInfo.get("mixed"));
                    }
                }
            } else {
                // Handle unqualified type names
                if (complexTypes.containsKey(type)) {
                    Map<String, Object> typeInfo = complexTypes.get(type);
                    
                    // Include child elements from the complex type
                    if (typeInfo.containsKey("elements")) {
                        elementInfo.put("elements", typeInfo.get("elements"));
                    }
                    
                    // Include attributes from the complex type
                    if (typeInfo.containsKey("attributes")) {
                        elementInfo.put("attributes", typeInfo.get("attributes"));
                    }
                    
                    // Include mixed content information
                    if (typeInfo.containsKey("mixed")) {
                        elementInfo.put("mixed", typeInfo.get("mixed"));
                    }
                }
            }
        }
        
        // Handle minOccurs and maxOccurs
        String minOccurs = element.getAttribute("minOccurs");
        if (!minOccurs.isEmpty()) {
            elementInfo.put("minOccurs", minOccurs);
        }
        
        String maxOccurs = element.getAttribute("maxOccurs");
        if (!maxOccurs.isEmpty()) {
            elementInfo.put("maxOccurs", maxOccurs);
        }
        
        // Handle all attributes
        NamedNodeMap attributes = element.getAttributes();
        if (attributes != null) {
            for (int i = 0; i < attributes.getLength(); i++) {
                Node attr = attributes.item(i);
                String attrName = attr.getLocalName();
                String value = attr.getNodeValue();
                if (!value.isEmpty() && !attrName.equals("name") && !attrName.equals("type") 
                        && !attrName.equals("minOccurs") && !attrName.equals("maxOccurs")) {
                    elementInfo.put(attrName, value);
                }
            }
        }
        
        // Handle annotation/documentation if present
        NodeList annotations = element.getElementsByTagNameNS(element.getNamespaceURI(), "annotation");
        if (annotations.getLength() > 0) {
            Element annotation = (Element) annotations.item(0);
            NodeList docs = annotation.getElementsByTagNameNS(element.getNamespaceURI(), "documentation");
            if (docs.getLength() > 0) {
                elementInfo.put("documentation", docs.item(0).getTextContent().trim());
            }
        }
        
        // Process complex type if present
        NodeList complexTypeList = element.getElementsByTagNameNS(element.getNamespaceURI(), "complexType");
        if (complexTypeList.getLength() > 0) {
            Element complexType = (Element) complexTypeList.item(0);
            elementInfo.put("hasComplexType", true);
            
            // Check for mixed content
            String mixed = complexType.getAttribute("mixed");
            if (mixed != null && !mixed.isEmpty()) {
                elementInfo.put("mixed", Boolean.parseBoolean(mixed));
            }
            
            // Process attributes
            List<Map<String, Object>> elementAttributes = processAttributes(complexType);
            if (!elementAttributes.isEmpty()) {
                elementInfo.put("attributes", elementAttributes);
            }
            
            // Process sequence, choice, or all elements
            for (String compositor : new String[]{"sequence", "choice", "all"}) {
                NodeList compositors = complexType.getElementsByTagNameNS(element.getNamespaceURI(), compositor);
                if (compositors.getLength() > 0) {
                    Element compositorElement = (Element) compositors.item(0);
                    elementInfo.put("compositor", compositor);
                    
                    NodeList childElements = compositorElement.getElementsByTagNameNS(element.getNamespaceURI(), "element");
                    List<Map<String, Object>> children = new ArrayList<>();
                    
                    for (int i = 0; i < childElements.getLength(); i++) {
                        Element childElement = (Element) childElements.item(i);
                        // Only process immediate children
                        if (childElement.getParentNode().equals(compositorElement)) {
                            children.add(processElement(childElement, complexTypes, namespaceContext));
                        }
                    }
                    
                    elementInfo.put("elements", children);
                    break; // Only process the first compositor found
                }
            }
        }
        
        // Process simple type if present
        NodeList simpleTypes = element.getElementsByTagNameNS(element.getNamespaceURI(), "simpleType");
        if (simpleTypes.getLength() > 0) {
            Element simpleType = (Element) simpleTypes.item(0);
            elementInfo.put("hasSimpleType", true);
            
            // Process restrictions
            NodeList restrictions = simpleType.getElementsByTagNameNS(element.getNamespaceURI(), "restriction");
            if (restrictions.getLength() > 0) {
                Element restriction = (Element) restrictions.item(0);
                elementInfo.put("baseType", restriction.getAttribute("base"));
                
                // Process facets (enumeration, pattern, etc.)
                NodeList facets = restriction.getChildNodes();
                List<Map<String, String>> facetList = new ArrayList<>();
                for (int i = 0; i < facets.getLength(); i++) {
                    Node facet = facets.item(i);
                    if (facet.getNodeType() == Node.ELEMENT_NODE) {
                        Map<String, String> facetInfo = new HashMap<>();
                        facetInfo.put("type", facet.getLocalName());
                        facetInfo.put("value", ((Element) facet).getAttribute("value"));
                        facetList.add(facetInfo);
                    }
                }
                if (!facetList.isEmpty()) {
                    elementInfo.put("facets", facetList);
                }
            }
        }
        
        return elementInfo;
    }

    @Override
    public List<Map<String, Object>> getXsdStructure(String xsdPath, Long clientId) {
        List<Map<String, Object>> structure = getXsdStructure(xsdPath);
        
        // Add client-specific customizations
        List<MappingRule> clientRules = mappingRuleRepository.findByClient_Id(clientId);
        for (MappingRule rule : clientRules) {
            Map<String, Object> ruleInfo = new HashMap<>();
            ruleInfo.put("name", rule.getName());
            
            // Support both old and new field names
            if (rule.getXmlPath() != null && !rule.getXmlPath().isEmpty()) {
                ruleInfo.put("xmlPath", rule.getXmlPath());
            } else if (rule.getSourceField() != null && !rule.getSourceField().isEmpty()) {
                ruleInfo.put("xmlPath", rule.getSourceField());
            }
            
            if (rule.getDatabaseField() != null && !rule.getDatabaseField().isEmpty()) {
                ruleInfo.put("databaseField", rule.getDatabaseField());
            } else if (rule.getTargetField() != null && !rule.getTargetField().isEmpty()) {
                ruleInfo.put("databaseField", rule.getTargetField());
            }
            
            ruleInfo.put("type", "mapping_rule");
            structure.add(ruleInfo);
        }
        
        return structure;
    }

    @Override
    public Page<MappingRule> getAllMappingRules(Pageable pageable) {
        return mappingRuleRepository.findAll(pageable);
    }

    @Override
    public Page<MappingRule> getMappingRulesByClient(Long clientId, Pageable pageable) {
        return mappingRuleRepository.findByClient_Id(clientId, pageable);
    }

    @Override
    public Page<MappingRule> getMappingRulesByInterface(Long interfaceId, Pageable pageable) {
        return mappingRuleRepository.findByInterfaceId(interfaceId, pageable);
    }

    @Override
    public Page<MappingRule> getMappingRulesByClientAndInterface(Long clientId, Long interfaceId, Pageable pageable) {
        return circuitBreakerService.<Page<MappingRule>>executeRepositoryOperation(
            () -> {
                try {
                    // First find rules by interface ID with pagination
                    Page<MappingRule> rules = mappingRuleRepository.findByInterfaceEntity_Id(interfaceId, pageable);
                    // Then filter by client ID if needed
                    if (clientId != null) {
                        return new PageImpl<>(
                            rules.getContent().stream()
                                .filter(rule -> rule.getClient().getId().equals(clientId))
                                .toList(),
                            pageable,
                            rules.getTotalElements()
                        );
                    }
                    return rules;
                } catch (Exception e) {
                    log.error("Error retrieving mapping rules for client {} and interface {}: {}", 
                            clientId, interfaceId, e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Returning empty page for getMappingRulesByClientAndInterface");
                return Page.empty(pageable);
            }
        );
    }

    @Override
    public void deleteMappingRule(Long id) {
        mappingRuleRepository.deleteById(id);
    }

    @Override
    public List<MappingRule> getActiveMappingRules(Long interfaceId) {
        return mappingRuleRepository.findByInterfaceIdAndIsActiveTrue(interfaceId);
    }

    @Override
    public List<MappingRule> findByTableNameAndClient_Id(String tableName, Long clientId) {
        return mappingRuleRepository.findByTableNameAndClient_Id(tableName, clientId);
    }

    @Override
    public void deleteByClient_IdAndTableName(Long clientId, String tableName) {
        mappingRuleRepository.deleteByClient_IdAndTableName(clientId, tableName);
    }

    @Override
    public String analyzeXsdStructure(MultipartFile file) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(file.getInputStream());
            Element root = document.getDocumentElement();
            
            StringBuilder analysis = new StringBuilder();
            analysis.append("XSD Schema Analysis:\n");
            analysis.append("Root Element: ").append(root.getLocalName()).append("\n");
            analysis.append("Namespace: ").append(root.getNamespaceURI()).append("\n");
            
            // Extract and display all namespaces
            Map<String, String> namespaces = extractNamespaces(root);
            analysis.append("\nNamespaces:\n");
            for (Map.Entry<String, String> entry : namespaces.entrySet()) {
                String prefix = entry.getKey();
                if ("_default_".equals(prefix)) {
                    analysis.append("- Default: ").append(entry.getValue()).append("\n");
                } else {
                    analysis.append("- ").append(prefix).append(": ").append(entry.getValue()).append("\n");
                }
            }
            
            // Analyze child elements
            NodeList elements = document.getElementsByTagNameNS(root.getNamespaceURI(), "element");
            analysis.append("\nElements found: ").append(elements.getLength()).append("\n");
            
            Set<String> processedElements = new HashSet<>();
            for (int i = 0; i < elements.getLength(); i++) {
                Element element = (Element) elements.item(i);
                String name = element.getAttribute("name");
                
                // Skip duplicates
                if (processedElements.contains(name)) {
                    continue;
                }
                processedElements.add(name);
                
                analysis.append("- ").append(name);
                
                String type = element.getAttribute("type");
                if (!type.isEmpty()) {
                    analysis.append(" (").append(type).append(")");
                }
                
                analysis.append("\n");
            }
            
            // Analyze complex types
            NodeList complexTypes = document.getElementsByTagNameNS(root.getNamespaceURI(), "complexType");
            analysis.append("\nComplex Types found: ").append(complexTypes.getLength()).append("\n");
            
            Set<String> processedTypes = new HashSet<>();
            for (int i = 0; i < complexTypes.getLength(); i++) {
                Element complexType = (Element) complexTypes.item(i);
                String name = complexType.getAttribute("name");
                
                // Skip anonymous complex types
                if (name.isEmpty()) {
                    continue;
                }
                
                // Skip duplicates
                if (processedTypes.contains(name)) {
                    continue;
                }
                processedTypes.add(name);
                
                analysis.append("- ").append(name);
                
                // Check for mixed content
                String mixed = complexType.getAttribute("mixed");
                if ("true".equals(mixed)) {
                    analysis.append(" (mixed content)");
                }
                
                analysis.append("\n");
            }
            
            // Analyze attributes
            NodeList attributes = document.getElementsByTagNameNS(root.getNamespaceURI(), "attribute");
            analysis.append("\nAttributes found: ").append(attributes.getLength()).append("\n");
            
            Set<String> processedAttributes = new HashSet<>();
            for (int i = 0; i < attributes.getLength(); i++) {
                Element attribute = (Element) attributes.item(i);
                String name = attribute.getAttribute("name");
                
                // Skip duplicates
                if (processedAttributes.contains(name)) {
                    continue;
                }
                processedAttributes.add(name);
                
                analysis.append("- ").append(name);
                
                String type = attribute.getAttribute("type");
                if (!type.isEmpty()) {
                    analysis.append(" (").append(type).append(")");
                }
                
                String use = attribute.getAttribute("use");
                if ("required".equals(use)) {
                    analysis.append(" [required]");
                }
                
                analysis.append("\n");
            }
            
            // Check for processing instructions
            NodeList processingInstructions = document.getChildNodes();
            int piCount = 0;
            for (int i = 0; i < processingInstructions.getLength(); i++) {
                Node node = processingInstructions.item(i);
                if (node.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE) {
                    piCount++;
                }
            }
            
            if (piCount > 0) {
                analysis.append("\nProcessing Instructions found: ").append(piCount).append("\n");
            }
            
            return analysis.toString();
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new ValidationException("Failed to analyze XSD structure", e);
        }
    }

    @Override
    public String analyzeXsdStructureWithClient(MultipartFile file, Long clientId) {
        try {
            StringBuilder clientAnalysis = new StringBuilder(analyzeXsdStructure(file));
            
            // Add client-specific validation rules or customizations
            List<MappingRule> clientRules = mappingRuleRepository.findByClient_Id(clientId);
            clientAnalysis.append("\nClient-specific Mapping Rules:\n");
            for (MappingRule rule : clientRules) {
                clientAnalysis.append("- ").append(rule.getName())
                    .append(" (");
                
                // Support both old and new field names
                if (rule.getXmlPath() != null && !rule.getXmlPath().isEmpty()) {
                    clientAnalysis.append(rule.getXmlPath());
                } else if (rule.getSourceField() != null && !rule.getSourceField().isEmpty()) {
                    clientAnalysis.append(rule.getSourceField());
                }
                
                clientAnalysis.append(" -> ");
                
                if (rule.getDatabaseField() != null && !rule.getDatabaseField().isEmpty()) {
                    clientAnalysis.append(rule.getDatabaseField());
                } else if (rule.getTargetField() != null && !rule.getTargetField().isEmpty()) {
                    clientAnalysis.append(rule.getTargetField());
                }
                
                clientAnalysis.append(")\n");
            }
            
            return clientAnalysis.toString();
            
        } catch (Exception e) {
            throw new ValidationException("Failed to analyze XSD structure with client context", e);
        }
    }

    @Override
    public List<MappingRule> getMappingRules(Long clientId) {
        return mappingRuleRepository.findByClient_Id(clientId);
    }

    @Override
    public List<MappingRule> getMappingRulesByTableName(String tableName) {
        return mappingRuleRepository.findByTableNameAndClient_Id(tableName, null);
    }

    @Override
    public List<MappingRule> getActiveMappingRules(Long clientId, Pageable pageable) {
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                try {
                    // Get active rules by interface ID
                    List<MappingRule> rules = mappingRuleRepository.findByInterfaceEntity_IdAndIsActive(clientId, true);
                    
                    // Apply pagination manually
                    int start = (int) pageable.getOffset();
                    int end = Math.min((start + pageable.getPageSize()), rules.size());
                    return rules.subList(start, end);
                } catch (Exception e) {
                    log.error("Error retrieving active mapping rules for client {}: {}", 
                            clientId, e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Returning empty list for getActiveMappingRules");
                return new ArrayList<>();
            }
        );
    }

    @Override
    public void deleteMappingRulesByClientAndTable(Long clientId, String tableName) {
        mappingRuleRepository.deleteByClient_IdAndTableName(clientId, tableName);
    }

    @Override
    public List<Map<String, String>> getXsdNamespaces(Long interfaceId) {
        try {
            // Get the interface entity using InterfaceService
            Interface interfaceEntity = interfaceService.getInterfaceById(interfaceId)
                .orElseThrow(() -> new ValidationException("Interface not found with id: " + interfaceId));
            
            String xsdPath = interfaceEntity.getSchemaPath();
            if (xsdPath == null || xsdPath.isEmpty()) {
                log.warn("No schema path found for interface {}", interfaceId);
                return new ArrayList<>();
            }
            
            // Get the XSD structure
            List<Map<String, Object>> structure = getXsdStructure(xsdPath);
            
            // Extract namespaces from the schema info
            if (!structure.isEmpty() && structure.get(0).containsKey("namespaces")) {
                @SuppressWarnings("unchecked")
                List<Map<String, String>> namespaces = (List<Map<String, String>>) structure.get(0).get("namespaces");
                return namespaces;
            }
            
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Failed to get XSD namespaces for interface {}: {}", interfaceId, e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public Map<String, Object> analyzeXsdFile(MultipartFile file) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // Get root element
            String rootElement = getRootElement(file);
            result.put("rootElement", rootElement);
            
            // Get namespaces
            List<Map<String, String>> namespaces = getAllNamespaces(file);
            result.put("namespaces", namespaces);
            
            // Get structure
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(file.getInputStream());
            Element root = document.getDocumentElement();
            
            // Extract all namespaces
            final Map<String, String> namespaceMap = extractNamespaces(root);
            
            // Create a namespace context for XPath evaluation
            NamespaceContext namespaceContext = new NamespaceContext() {
                @Override
                public String getNamespaceURI(String prefix) {
                    return namespaceMap.getOrDefault(prefix, XMLConstants.NULL_NS_URI);
                }
                
                @Override
                public String getPrefix(String namespaceURI) {
                    for (Map.Entry<String, String> entry : namespaceMap.entrySet()) {
                        if (entry.getValue().equals(namespaceURI)) {
                            return entry.getKey();
                        }
                    }
                    return null;
                }
                
                @Override
                public Iterator<String> getPrefixes(String namespaceURI) {
                    List<String> prefixes = new ArrayList<>();
                    for (Map.Entry<String, String> entry : namespaceMap.entrySet()) {
                        if (entry.getValue().equals(namespaceURI)) {
                            prefixes.add(entry.getKey());
                        }
                    }
                    return prefixes.iterator();
                }
            };
            
            // Process complex types
            Map<String, Map<String, Object>> complexTypes = processComplexTypes(document, root, namespaceContext);
            
            List<Map<String, Object>> structure = new ArrayList<>();
            
            // Add schema info
            Map<String, Object> schemaInfo = new HashMap<>();
            schemaInfo.put("name", root.getLocalName());
            schemaInfo.put("type", "schema");
            schemaInfo.put("namespace", root.getNamespaceURI());
            
            // Add namespaces to schema info
            List<Map<String, String>> namespacesList = new ArrayList<>();
            for (Map.Entry<String, String> entry : namespaceMap.entrySet()) {
                Map<String, String> ns = new HashMap<>();
                ns.put("prefix", entry.getKey());
                ns.put("uri", entry.getValue());
                namespacesList.add(ns);
            }
            schemaInfo.put("namespaces", namespacesList);
            
            // Add any schema-level attributes
            NamedNodeMap attributes = root.getAttributes();
            if (attributes != null && attributes.getLength() > 0) {
                Map<String, String> schemaAttributes = new HashMap<>();
                for (int i = 0; i < attributes.getLength(); i++) {
                    Node attr = attributes.item(i);
                    if (!attr.getNodeName().startsWith("xmlns")) {
                        schemaAttributes.put(attr.getLocalName(), attr.getNodeValue());
                    }
                }
                schemaInfo.put("attributes", schemaAttributes);
            }
            structure.add(schemaInfo);
            
            // Get all global element declarations
            NodeList elements = document.getElementsByTagNameNS(root.getNamespaceURI(), "element");
            for (int i = 0; i < elements.getLength(); i++) {
                Element element = (Element) elements.item(i);
                // Only process global elements (those that are direct children of schema)
                if (element.getParentNode().equals(root)) {
                    Map<String, Object> elementInfo = processElement(element, complexTypes, namespaceContext);
                    structure.add(elementInfo);
                }
            }
            
            result.put("structure", structure);
            
            return result;
        } catch (Exception e) {
            throw new ValidationException("Failed to analyze XSD file: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Map<String, Object>> getComplexTypes(String xsdPath) {
        try {
            // Load the XSD document
            InputStream inputStream = getXsdInputStream(xsdPath);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);
            Element root = document.getDocumentElement();
            
            // Extract all namespaces
            final Map<String, String> namespaceMap = extractNamespaces(root);
            
            // Create a namespace context for XPath evaluation
            NamespaceContext namespaceContext = new NamespaceContext() {
                @Override
                public String getNamespaceURI(String prefix) {
                    return namespaceMap.getOrDefault(prefix, XMLConstants.NULL_NS_URI);
                }
                
                @Override
                public String getPrefix(String namespaceURI) {
                    for (Map.Entry<String, String> entry : namespaceMap.entrySet()) {
                        if (entry.getValue().equals(namespaceURI)) {
                            return entry.getKey();
                        }
                    }
                    return null;
                }
                
                @Override
                public Iterator<String> getPrefixes(String namespaceURI) {
                    List<String> prefixes = new ArrayList<>();
                    for (Map.Entry<String, String> entry : namespaceMap.entrySet()) {
                        if (entry.getValue().equals(namespaceURI)) {
                            prefixes.add(entry.getKey());
                        }
                    }
                    return prefixes.iterator();
                }
            };
            
            // Process complex types
            return processComplexTypes(document, root, namespaceContext);
        } catch (Exception e) {
            throw new ValidationException("Failed to get complex types: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Map<String, Object>> getXsdAttributes(String xsdPath) {
        try {
            // Load the XSD document
            InputStream inputStream = getXsdInputStream(xsdPath);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);
            Element root = document.getDocumentElement();
            
            List<Map<String, Object>> attributes = new ArrayList<>();
            
            // Get all attribute declarations
            NodeList attributeNodes = document.getElementsByTagNameNS(root.getNamespaceURI(), "attribute");
            for (int i = 0; i < attributeNodes.getLength(); i++) {
                Element attribute = (Element) attributeNodes.item(i);
                
                Map<String, Object> attrInfo = new HashMap<>();
                attrInfo.put("name", attribute.getAttribute("name"));
                
                String type = attribute.getAttribute("type");
                if (type != null && !type.isEmpty()) {
                    attrInfo.put("type", type);
                }
                
                String use = attribute.getAttribute("use");
                if (use != null && !use.isEmpty()) {
                    attrInfo.put("required", "required".equals(use));
                }
                
                String defaultValue = attribute.getAttribute("default");
                if (defaultValue != null && !defaultValue.isEmpty()) {
                    attrInfo.put("default", defaultValue);
                }
                
                // Get parent element name
                Node parent = attribute.getParentNode();
                if (parent.getNodeType() == Node.ELEMENT_NODE) {
                    Element parentElement = (Element) parent;
                    if ("attributeGroup".equals(parentElement.getLocalName())) {
                        attrInfo.put("attributeGroup", parentElement.getAttribute("name"));
                    } else if ("complexType".equals(parentElement.getLocalName())) {
                        attrInfo.put("complexType", parentElement.getAttribute("name"));
                    }
                }
                
                attributes.add(attrInfo);
            }
            
            return attributes;
        } catch (Exception e) {
            throw new ValidationException("Failed to get XSD attributes: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean hasMixedContent(String xsdPath) {
        try {
            // Load the XSD document
            InputStream inputStream = getXsdInputStream(xsdPath);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);
            Element root = document.getDocumentElement();
            
            // Check for mixed content in complex types
            NodeList complexTypes = document.getElementsByTagNameNS(root.getNamespaceURI(), "complexType");
            for (int i = 0; i < complexTypes.getLength(); i++) {
                Element complexType = (Element) complexTypes.item(i);
                String mixed = complexType.getAttribute("mixed");
                if ("true".equals(mixed)) {
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            throw new ValidationException("Failed to check for mixed content: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Map<String, String>> getProcessingInstructions(String xsdPath) {
        try {
            // Load the XSD document
            InputStream inputStream = getXsdInputStream(xsdPath);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);
            
            List<Map<String, String>> processingInstructions = new ArrayList<>();
            
            // Get all processing instructions
            NodeList nodes = document.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE) {
                    ProcessingInstruction pi = (ProcessingInstruction) node;
                    Map<String, String> piInfo = new HashMap<>();
                    piInfo.put("target", pi.getTarget());
                    piInfo.put("data", pi.getData());
                    processingInstructions.add(piInfo);
                }
            }
            
            return processingInstructions;
        } catch (Exception e) {
            throw new ValidationException("Failed to get processing instructions: " + e.getMessage(), e);
        }
    }

    /**
     * Helper method to get an InputStream for an XSD file.
     */
    private InputStream getXsdInputStream(String xsdPath) throws IOException {
        if (xsdPath == null || xsdPath.trim().isEmpty()) {
            throw new ValidationException("XSD path cannot be null or empty");
        }

        // Normalize path separators to forward slashes
        String normalizedPath = xsdPath.replace("\\", "/");
        
        // Handle relative path from resources directory
        String resourcePath = normalizedPath;
        if (normalizedPath.startsWith("backend/src/main/resources/")) {
            resourcePath = normalizedPath.substring("backend/src/main/resources/".length());
        }
        
        // Try loading from classpath first
        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            return resource.getInputStream();
        } catch (IOException e) {
            // If not found in classpath, try absolute path
            Path absolutePath = Paths.get(normalizedPath);
            if (Files.exists(absolutePath)) {
                return Files.newInputStream(absolutePath);
            } else {
                // Try one more time with just the filename from resources
                try {
                    String fileName = Paths.get(resourcePath).getFileName().toString();
                    ClassPathResource resource = new ClassPathResource(fileName);
                    return resource.getInputStream();
                } catch (IOException e2) {
                    throw new ValidationException(
                        String.format("XSD file not found. Tried:\n" +
                                    "- Classpath: %s\n" +
                                    "- Absolute path: %s\n" +
                                    "- Resources filename: %s",
                                    resourcePath, normalizedPath, Paths.get(resourcePath).getFileName())
                    );
                }
            }
        }
    }
}
