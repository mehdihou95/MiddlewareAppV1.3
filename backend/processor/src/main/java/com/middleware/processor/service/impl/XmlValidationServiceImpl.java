package com.middleware.processor.service.impl;

import com.middleware.processor.config.XmlValidationConfig;
import com.middleware.processor.exception.XmlValidationException;
import com.middleware.processor.service.interfaces.XmlValidationService;
import com.middleware.shared.model.Interface;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced implementation of XmlValidationService.
 * Provides comprehensive XML validation with proper namespace handling and schema matching.
 */
@Slf4j
@Service
public class XmlValidationServiceImpl implements XmlValidationService {
    
    private final XmlValidationConfig validationConfig;
    private String validationErrorMessage;

    public XmlValidationServiceImpl(XmlValidationConfig validationConfig) {
        this.validationConfig = validationConfig;
        // Set system properties once during initialization
        configureSystemProperties();
    }
    
    /**
     * Configure system properties for XML parsing
     */
    private void configureSystemProperties() {
        System.setProperty("jdk.xml.entityExpansionLimit", validationConfig.getEntityExpansionLimit());
        System.setProperty("entityExpansionLimit", validationConfig.getEntityExpansionLimit());
        System.setProperty("jdk.xml.maxOccurLimit", validationConfig.getEntityExpansionLimit());
        System.setProperty("javax.xml.accessExternalDTD", validationConfig.isEnableExternalDtd() ? "all" : "");
        System.setProperty("javax.xml.accessExternalSchema", validationConfig.isEnableExternalSchema() ? "all" : "");
        
        log.debug("XML validation system properties configured: entityExpansionLimit={}, externalDTD={}, externalSchema={}",
                 validationConfig.getEntityExpansionLimit(),
                 validationConfig.isEnableExternalDtd(),
                 validationConfig.isEnableExternalSchema());
    }
    
    /**
     * Configure a SchemaFactory with validation settings
     * 
     * @param factory The SchemaFactory to configure
     */
    private void configureSchemaFactory(SchemaFactory factory) {
        try {
            // Enable external schema access
            System.setProperty("javax.xml.accessExternalSchema", "all");
            System.setProperty("javax.xml.accessExternalDTD", "all");
            
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "all");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "all");
            
            try {
                factory.setProperty("http://www.oracle.com/xml/jaxp/properties/entityExpansionLimit", 
                                  validationConfig.getEntityExpansionLimit());
            } catch (SAXException e) {
                log.debug("Could not set Oracle entity expansion limit", e);
            }
            
            try {
                factory.setProperty("entityExpansionLimit", validationConfig.getEntityExpansionLimit());
            } catch (SAXException e) {
                log.debug("Could not set direct entity expansion limit", e);
            }

            // Set XML validation features from config
            try {
                factory.setFeature("http://apache.org/xml/features/honour-all-schemaLocations", 
                                 validationConfig.isHonourAllSchemaLocations());
                factory.setFeature("http://apache.org/xml/features/validation/schema-full-checking", 
                                 validationConfig.isEnableSchemaFullChecking());
                
                if (validationConfig.getAdditionalFeatures() != null) {
                    for (Map.Entry<String, Boolean> feature : validationConfig.getAdditionalFeatures().entrySet()) {
                        factory.setFeature(feature.getKey(), feature.getValue());
                    }
                }
            } catch (SAXException e) {
                log.debug("Could not set XML features", e);
            }
            
            log.debug("Configured SchemaFactory with external access enabled");
        } catch (Exception e) {
            log.error("Error configuring SchemaFactory: " + e.getMessage(), e);
        }
    }
    
    /**
     * Configure a Validator with validation settings
     * 
     * @param validator The Validator to configure
     */
    private void configureValidator(Validator validator) {
        try {
            validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, 
                                validationConfig.isEnableExternalDtd() ? "all" : "");
            validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, 
                                validationConfig.isEnableExternalSchema() ? "all" : "");
            log.trace("Configured validator security settings");
        } catch (SAXException e) {
            log.debug("Could not set validator security properties", e);
        }
        
        try {
            validator.setProperty("http://www.oracle.com/xml/jaxp/properties/entityExpansionLimit", 
                                validationConfig.getEntityExpansionLimit());
            log.trace("Set validator entity expansion limit");
        } catch (SAXException e) {
            log.debug("Could not set Oracle entity expansion limit on validator", e);
        }
        
        try {
            validator.setFeature("http://apache.org/xml/features/validation/schema-full-checking", 
                               validationConfig.isEnableSchemaFullChecking());
            log.trace("Set validator schema full checking feature");
        } catch (SAXException e) {
            log.debug("Could not set validator features", e);
        }
    }

    @Override
    public boolean validateXmlAgainstXsd(Document document, String xsdContent) {
        // First check if the XML is compatible with the XSD
        if (!isXmlCompatibleWithXsd(document, xsdContent)) {
            validationErrorMessage = "XML document is not compatible with the XSD schema. Root element or namespace mismatch.";
            log.error(validationErrorMessage);
            return false;
        }
        
        return validateXmlAgainstXsdWithNamespaceCheck(document, xsdContent, true);
    }
    
    @Override
    public boolean validateXmlAgainstXsdWithNamespaceCheck(Document document, String xsdContent, boolean enforceNamespaces) {
        try {
            log.trace("Starting XML validation against XSD");
            Element rootElement = document.getDocumentElement();
            log.trace("XML document root element: {}", rootElement.getNodeName());
            log.trace("XML document root namespace: {}", rootElement.getNamespaceURI());
            log.trace("XSD content length: {}", xsdContent.length());
            
            // Create and configure the schema factory
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            log.trace("Created SchemaFactory instance");
            
            // Configure the factory
            configureSchemaFactory(factory);
            
            Source schemaSource = new StreamSource(new StringReader(xsdContent));
            log.trace("Created schema source from XSD content");
            
            Schema schema = factory.newSchema(schemaSource);
            log.trace("Created schema from source");
            
            Validator validator = schema.newValidator();
            log.trace("Created validator from schema");
            
            // Configure the validator
            configureValidator(validator);
            
            // Add custom error handler for detailed error messages
            final StringBuilder errorDetails = new StringBuilder();
            validator.setErrorHandler(new ErrorHandler() {
                @Override
                public void warning(SAXParseException e) {
                    log.warn("XML validation warning: " + e.getMessage());
                }

                @Override
                public void error(SAXParseException e) throws SAXException {
                    errorDetails.append("Line ").append(e.getLineNumber())
                               .append(", Column ").append(e.getColumnNumber())
                               .append(": ").append(e.getMessage()).append("\n");
                    throw e;
                }

                @Override
                public void fatalError(SAXParseException e) throws SAXException {
                    errorDetails.append("FATAL: Line ").append(e.getLineNumber())
                               .append(", Column ").append(e.getColumnNumber())
                               .append(": ").append(e.getMessage()).append("\n");
                    throw e;
                }
            });
            
            // Add debug logging
            log.debug("Starting validation with entity expansion limit: {}", 
                     validationConfig.getEntityExpansionLimit());
            validator.validate(new DOMSource(document));
            log.trace("Validation completed successfully");
            
            validationErrorMessage = null;
            return true;
        } catch (SAXException e) {
            validationErrorMessage = "XML validation failed against XSD: " + e.getMessage();
            log.error(validationErrorMessage, e);
            log.error("Validation stack trace:", e);
            return false;
        } catch (IOException e) {
            validationErrorMessage = "Error reading XSD schema: " + e.getMessage();
            log.error(validationErrorMessage, e);
            return false;
        }
    }

    @Override
    public boolean validateXmlStructure(Document document) {
        try {
            Element root = document.getDocumentElement();
            if (root == null) {
                validationErrorMessage = "XML document has no root element";
                return false;
            }

            // Check for well-formedness
            if (!isWellFormed(document)) {
                return false;
            }

            validationErrorMessage = null;
            return true;
        } catch (Exception e) {
            validationErrorMessage = "XML structure validation failed: " + e.getMessage();
            log.error(validationErrorMessage, e);
            return false;
        }
    }
    
    /**
     * Check if a document is well-formed
     */
    private boolean isWellFormed(Document document) {
        try {
            // Check for basic structure issues
            if (document.getDocumentElement() == null) {
                validationErrorMessage = "XML document has no root element";
                return false;
            }
            
            // Check for namespace consistency
            Map<String, String> declaredNamespaces = extractNamespaces(document.getDocumentElement());
            List<String> undeclaredPrefixes = findUndeclaredPrefixes(document.getDocumentElement(), declaredNamespaces);
            
            if (!undeclaredPrefixes.isEmpty()) {
                validationErrorMessage = "XML document uses undeclared namespace prefixes: " + String.join(", ", undeclaredPrefixes);
                return false;
            }
            
            return true;
        } catch (Exception e) {
            validationErrorMessage = "XML well-formedness check failed: " + e.getMessage();
            log.error(validationErrorMessage, e);
            return false;
        }
    }
    
    /**
     * Extract all namespace declarations from an element and its ancestors
     */
    private Map<String, String> extractNamespaces(Element element) {
        Map<String, String> namespaces = new HashMap<>();
        
        // Process the element and all its ancestors
        Element current = element;
        while (current != null) {
            // Get attributes which are namespace declarations
            for (int i = 0; i < current.getAttributes().getLength(); i++) {
                org.w3c.dom.Node attr = current.getAttributes().item(i);
                String name = attr.getNodeName();
                
                if (name.equals("xmlns")) {
                    // Default namespace
                    namespaces.put("", attr.getNodeValue());
                } else if (name.startsWith("xmlns:")) {
                    // Prefixed namespace
                    String prefix = name.substring(6); // after "xmlns:"
                    namespaces.put(prefix, attr.getNodeValue());
                }
            }
            
            // Move to parent element
            org.w3c.dom.Node parent = current.getParentNode();
            if (parent != null && parent.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                current = (Element) parent;
            } else {
                current = null;
            }
        }
        
        return namespaces;
    }
    
    /**
     * Find all namespace prefixes used in an element and its descendants that are not declared
     */
    private List<String> findUndeclaredPrefixes(Element element, Map<String, String> declaredNamespaces) {
        List<String> undeclaredPrefixes = new ArrayList<>();
        
        // Check the element itself
        String prefix = element.getPrefix();
        if (prefix != null && !prefix.isEmpty() && !declaredNamespaces.containsKey(prefix)) {
            undeclaredPrefixes.add(prefix);
        }
        
        // Check attributes
        for (int i = 0; i < element.getAttributes().getLength(); i++) {
            org.w3c.dom.Node attr = element.getAttributes().item(i);
            String attrPrefix = attr.getPrefix();
            if (attrPrefix != null && !attrPrefix.isEmpty() && !attrPrefix.equals("xmlns") && 
                !declaredNamespaces.containsKey(attrPrefix)) {
                undeclaredPrefixes.add(attrPrefix);
            }
        }
        
        // Check child elements
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            org.w3c.dom.Node child = children.item(i);
            if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                // Combine declared namespaces from this level with those from ancestors
                Map<String, String> combinedNamespaces = new HashMap<>(declaredNamespaces);
                combinedNamespaces.putAll(extractNamespaces((Element) child));
                
                // Recursively check child elements
                undeclaredPrefixes.addAll(findUndeclaredPrefixes((Element) child, combinedNamespaces));
            }
        }
        
        return undeclaredPrefixes;
    }

    @Override
    public boolean validateXmlContent(Document document, Interface interfaceEntity) {
        try {
            String rootElement = interfaceEntity.getRootElement();
            boolean isFlexibleMode = rootElement != null && rootElement.toUpperCase().endsWith(":FLEXIBLE");
            
            // Perform validation based on mode
            return performGenericValidation(document, interfaceEntity, isFlexibleMode);
        } catch (Exception e) {
            validationErrorMessage = "XML validation failed: " + e.getMessage();
            log.error(validationErrorMessage, e);
            return false;
        }
    }

    private boolean performGenericValidation(Document document, Interface interfaceEntity, boolean isFlexibleMode) {
        try {
            // First validate basic XML structure for both modes
            if (!validateXmlStructure(document)) {
                return false;
            }

            if (isFlexibleMode) {
                // In flexible mode, only validate basic structure
                Element rootElement = document.getDocumentElement();
                String expectedRoot = interfaceEntity.getRootElement().split(":")[0]; // Remove :FLEXIBLE suffix
                
                // Check if root element name matches (ignoring namespace)
                if (!rootElement.getLocalName().equals(expectedRoot)) {
                    validationErrorMessage = String.format(
                        "Root element mismatch. XML has '%s' but expected '%s'", 
                        rootElement.getLocalName(), expectedRoot);
                    return false;
                }
                
                return true;
            }

            // For strict mode, do full schema validation
            Path xsdPath = Paths.get(interfaceEntity.getSchemaPath());
            if (!Files.exists(xsdPath)) {
                String resourcePath = interfaceEntity.getSchemaPath().replace("backend/src/main/resources/", "");
                URL resourceUrl = getClass().getClassLoader().getResource(resourcePath);
                if (resourceUrl != null) {
                    xsdPath = Paths.get(resourceUrl.toURI());
                } else {
                    validationErrorMessage = "XSD schema not found at path: " + xsdPath;
                    log.error(validationErrorMessage);
                    return false;
                }
            }

            // Create and configure schema factory
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            configureSchemaFactory(factory);

            // Enable all strict validation features
            factory.setFeature("http://apache.org/xml/features/validation/schema-full-checking", true);
            factory.setFeature("http://apache.org/xml/features/honour-all-schemaLocations", true);

            // Create schema and validator
            Source schemaSource = new StreamSource(Files.newInputStream(xsdPath));
            Schema schema = factory.newSchema(schemaSource);
            Validator validator = schema.newValidator();
            configureValidator(validator);

            // Set up error handler
            final StringBuilder errorDetails = new StringBuilder();
            validator.setErrorHandler(new ErrorHandler() {
                @Override
                public void warning(SAXParseException e) {
                    log.warn("Validation warning: " + e.getMessage());
                }

                @Override
                public void error(SAXParseException e) throws SAXException {
                    errorDetails.append("Line ").append(e.getLineNumber())
                            .append(", Column ").append(e.getColumnNumber())
                            .append(": ").append(e.getMessage()).append("\n");
                    throw e;
                }

                @Override
                public void fatalError(SAXParseException e) throws SAXException {
                    errorDetails.append("FATAL: Line ").append(e.getLineNumber())
                            .append(", Column ").append(e.getColumnNumber())
                            .append(": ").append(e.getMessage()).append("\n");
                    throw e;
                }
            });

            // Validate document
            validator.validate(new DOMSource(document));
            return true;

        } catch (Exception e) {
            validationErrorMessage = "XML validation failed: " + e.getMessage();
            log.error(validationErrorMessage, e);
            return false;
        }
    }

    @Override
    public boolean validateXmlContent(Document document, String interfaceType) {
        throw new UnsupportedOperationException("This method is deprecated. Please use validateXmlContent(Document, Interface) instead.");
    }

    @Override
    public String getValidationErrorMessage() {
        return validationErrorMessage;
    }

    @Override
    public void validateXmlContent(String xmlContent, String interfaceType) throws XmlValidationException {
        throw new UnsupportedOperationException("This method is deprecated. Please use validateXmlContent with Interface entity instead.");
    }

    @Override
    public boolean isXmlCompatibleWithXsd(Document document, String xsdContent) {
        try {
            // Extract root element info from XML document
            Element xmlRoot = document.getDocumentElement();
            String xmlRootName = xmlRoot.getLocalName();
            String xmlRootNamespace = xmlRoot.getNamespaceURI();
            
            log.debug("XML root element: {} in namespace: {}", xmlRootName, xmlRootNamespace);
            
            // Extract root element info from XSD
            Map<String, String> xsdRootInfo = extractXsdRootElementInfo(xsdContent);
            String xsdRootName = xsdRootInfo.get("name");
            String xsdRootNamespace = xsdRootInfo.get("namespace");
            
            log.debug("XSD root element: {} in namespace: {}", xsdRootName, xsdRootNamespace);
            
            // Check if root element names match
            if (!xmlRootName.equals(xsdRootName)) {
                validationErrorMessage = String.format(
                    "Root element mismatch. XML has '%s' but XSD expects '%s'", 
                    xmlRootName, xsdRootName);
                log.error(validationErrorMessage);
                return false;
            }
            
            // Check if namespaces are compatible
            if (xsdRootNamespace != null && !xsdRootNamespace.isEmpty()) {
                // XSD has a target namespace, XML must match it
                if (xmlRootNamespace == null || !xmlRootNamespace.equals(xsdRootNamespace)) {
                    validationErrorMessage = String.format(
                        "Namespace mismatch. XML has '%s' but XSD expects '%s'", 
                        xmlRootNamespace, xsdRootNamespace);
                    log.error(validationErrorMessage);
                    return false;
                }
            } else if (xmlRootNamespace != null && !xmlRootNamespace.isEmpty()) {
                // XSD has no target namespace but XML has one
                validationErrorMessage = "Namespace mismatch. XML has a namespace but XSD has none";
                log.error(validationErrorMessage);
                return false;
            }
            
            return true;
        } catch (Exception e) {
            validationErrorMessage = "Error checking XML compatibility with XSD: " + e.getMessage();
            log.error(validationErrorMessage, e);
            return false;
        }
    }
    
    @Override
    public boolean isXmlCompatibleWithInterface(Document document, Interface interfaceEntity) {
        try {
            // Extract root element info from XML document
            Element xmlRoot = document.getDocumentElement();
            String xmlRootName = xmlRoot.getLocalName();
            String xmlRootNamespace = xmlRoot.getNamespaceURI();
            
            // Get interface root element and namespace
            String interfaceRootElement = interfaceEntity.getRootElement();
            String interfaceNamespace = interfaceEntity.getNamespace();
            
            log.debug("XML root element: {} in namespace: {}", xmlRootName, xmlRootNamespace);
            log.debug("Interface expects root element: {} in namespace: {}", 
                     interfaceRootElement, interfaceNamespace);
            
            // Check if root element names match
            if (!xmlRootName.equals(interfaceRootElement)) {
                validationErrorMessage = String.format(
                    "Root element mismatch. XML has '%s' but interface expects '%s'", 
                    xmlRootName, interfaceRootElement);
                log.error(validationErrorMessage);
                return false;
            }
            
            // Check if namespaces are compatible
            if (interfaceNamespace != null && !interfaceNamespace.isEmpty()) {
                // Interface has a namespace, XML must match it
                if (xmlRootNamespace == null || !xmlRootNamespace.equals(interfaceNamespace)) {
                    validationErrorMessage = String.format(
                        "Namespace mismatch. XML has '%s' but interface expects '%s'", 
                        xmlRootNamespace, interfaceNamespace);
                    log.error(validationErrorMessage);
                    return false;
                }
            } else if (xmlRootNamespace != null && !xmlRootNamespace.isEmpty()) {
                // Interface has no namespace but XML has one
                validationErrorMessage = "Namespace mismatch. XML has a namespace but interface has none";
                log.error(validationErrorMessage);
                return false;
            }
            
            return true;
        } catch (Exception e) {
            validationErrorMessage = "Error checking XML compatibility with interface: " + e.getMessage();
            log.error(validationErrorMessage, e);
            return false;
        }
    }
    
    @Override
    public Map<String, String> extractXsdRootElementInfo(String xsdContent) {
        Map<String, String> rootInfo = new HashMap<>();
        
        try {
            // Parse the XSD content
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document xsdDoc = builder.parse(new InputSource(new StringReader(xsdContent)));
            
            // Get the schema element
            Element schemaElement = xsdDoc.getDocumentElement();
            
            // Get target namespace
            String targetNamespace = schemaElement.getAttribute("targetNamespace");
            rootInfo.put("namespace", targetNamespace);
            
            // Find the root element declaration
            NodeList elements = schemaElement.getElementsByTagNameNS(XMLConstants.W3C_XML_SCHEMA_NS_URI, "element");
            
            // Look for global element declarations (direct children of schema)
            for (int i = 0; i < elements.getLength(); i++) {
                Element element = (Element) elements.item(i);
                if (element.getParentNode().equals(schemaElement)) {
                    // This is a global element declaration
                    String name = element.getAttribute("name");
                    rootInfo.put("name", name);
                    
                    // We'll use the first global element as the root
                    break;
                }
            }
            
            // If no global elements found, try XPath
            if (!rootInfo.containsKey("name")) {
                XPathFactory xpathFactory = XPathFactory.newInstance();
                XPath xpath = xpathFactory.newXPath();
                
                // Set up namespace context for XPath
                xpath.setNamespaceContext(new NamespaceContext() {
                    @Override
                    public String getNamespaceURI(String prefix) {
                        if ("xs".equals(prefix)) {
                            return XMLConstants.W3C_XML_SCHEMA_NS_URI;
                        }
                        return XMLConstants.NULL_NS_URI;
                    }
                    
                    @Override
                    public String getPrefix(String namespaceURI) {
                        if (XMLConstants.W3C_XML_SCHEMA_NS_URI.equals(namespaceURI)) {
                            return "xs";
                        }
                        return null;
                    }
                    
                    @Override
                    public Iterator<String> getPrefixes(String namespaceURI) {
                        List<String> prefixes = new ArrayList<>();
                        if (XMLConstants.W3C_XML_SCHEMA_NS_URI.equals(namespaceURI)) {
                            prefixes.add("xs");
                        }
                        return prefixes.iterator();
                    }
                });
                
                // Try to find the first global element
                NodeList globalElements = (NodeList) xpath.evaluate(
                    "/xs:schema/xs:element", xsdDoc, XPathConstants.NODESET);
                
                if (globalElements.getLength() > 0) {
                    Element firstElement = (Element) globalElements.item(0);
                    rootInfo.put("name", firstElement.getAttribute("name"));
                }
            }
            
            return rootInfo;
        } catch (Exception e) {
            log.error("Error extracting root element info from XSD: " + e.getMessage(), e);
            // Return empty map with default values
            rootInfo.put("name", "");
            rootInfo.put("namespace", "");
            return rootInfo;
        }
    }
}
