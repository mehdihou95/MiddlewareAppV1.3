package com.middleware.processor.service.util;

import com.middleware.processor.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Comprehensive XML processing utility that handles XML parsing, manipulation,
 * namespace management, and XPath evaluation.
 */
@Component
public class XmlProcessor {
    private static final Logger logger = LoggerFactory.getLogger(XmlProcessor.class);
    
    // Pattern to match namespace prefixes in XPath expressions
    private static final Pattern PREFIX_PATTERN = Pattern.compile("([a-zA-Z0-9_]+):");
    
    // Map to store namespace URIs by prefix
    private final Map<String, String> namespaceMap = new HashMap<>();
    
    // Map to store prefixes by namespace URI (reverse lookup)
    private final Map<String, String> prefixMap = new HashMap<>();
    
    /**
     * Parse an XML document from an input stream with namespace awareness.
     * 
     * @param inputStream The input stream containing the XML document
     * @return The parsed Document object
     * @throws Exception If parsing fails
     */
    public Document parseXml(InputStream inputStream) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(inputStream);
        
        // Extract namespaces from the document
        extractNamespaces(document.getDocumentElement());
        
        return document;
    }
    
    /**
     * Parse an XML document from a string with namespace awareness.
     * 
     * @param xmlString The string containing the XML document
     * @return The parsed Document object
     * @throws Exception If parsing fails
     */
    public Document parseXml(String xmlString) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource inputSource = new InputSource(new StringReader(xmlString));
        Document document = builder.parse(inputSource);
        
        // Extract namespaces from the document
        extractNamespaces(document.getDocumentElement());
        
        return document;
    }
    
    /**
     * Extract all namespace declarations from an element and its children.
     * 
     * @param element The element to extract namespaces from
     */
    private void extractNamespaces(Element element) {
        // Get namespace declarations from this element
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            String name = attribute.getNodeName();
            String value = attribute.getNodeValue();
            
            if (name.startsWith("xmlns:")) {
                String prefix = name.substring(6); // Remove "xmlns:"
                namespaceMap.put(prefix, value);
                prefixMap.put(value, prefix);
                logger.debug("Found namespace: {}={}", prefix, value);
            } else if (name.equals("xmlns")) {
                // Default namespace
                namespaceMap.put("_default_", value);
                prefixMap.put(value, "_default_");
                logger.debug("Found default namespace: {}", value);
            }
        }
        
        // Process child elements recursively
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                extractNamespaces((Element) child);
            }
        }
    }
    
    /**
     * Get a map of all namespace prefixes to URIs found in the document.
     * 
     * @return Map of namespace prefixes to URIs
     */
    public Map<String, String> getNamespaces() {
        return new HashMap<>(namespaceMap);
    }
    
    /**
     * Create a namespace context for XPath evaluation that handles prefix mapping.
     * 
     * @return A NamespaceContext for XPath evaluation
     */
    private NamespaceContext createNamespaceContext() {
        return new NamespaceContext() {
            @Override
            public String getNamespaceURI(String prefix) {
                if (prefix == null) {
                    throw new IllegalArgumentException("Null prefix");
                }
                String uri = namespaceMap.get(prefix);
                return uri != null ? uri : "";
            }
            
            @Override
            public String getPrefix(String namespaceURI) {
                return prefixMap.get(namespaceURI);
            }
            
            @Override
            public Iterator<String> getPrefixes(String namespaceURI) {
                String prefix = prefixMap.get(namespaceURI);
                if (prefix == null) {
                    return java.util.Collections.emptyIterator();
                }
                return java.util.Collections.singletonList(prefix).iterator();
            }
        };
    }
    
    /**
     * Create an XPath object with namespace context for evaluating XPath expressions.
     * 
     * @return An XPath object with namespace context
     */
    public XPath createXPath() {
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(createNamespaceContext());
        return xpath;
    }
    
    /**
     * Evaluate an XPath expression on a document and return the result as a string.
     * 
     * @param document The document to evaluate the XPath on
     * @param xpathExpression The XPath expression to evaluate
     * @return The result as a string, or null if no match
     * @throws Exception If evaluation fails
     */
    public String evaluateXPath(Document document, String xpathExpression) throws Exception {
        // First try with prefixes as-is
        try {
            XPath xpath = createXPath();
            Node node = (Node) xpath.evaluate(xpathExpression, document, XPathConstants.NODE);
            if (node != null) {
                return node.getTextContent();
            }
        } catch (Exception e) {
            logger.debug("Failed to evaluate XPath with prefixes as-is: {}", e.getMessage());
        }
        
        // Try with prefix-trimmed XPath
        try {
            String trimmedXPath = trimPrefixes(xpathExpression);
            XPath xpath = createXPath();
            Node node = (Node) xpath.evaluate(trimmedXPath, document, XPathConstants.NODE);
            if (node != null) {
                return node.getTextContent();
            }
        } catch (Exception e) {
            logger.debug("Failed to evaluate XPath with trimmed prefixes: {}", e.getMessage());
        }
        
        // Try with local-name() approach
        try {
            String localNameXPath = convertToLocalNameXPath(xpathExpression);
            XPath xpath = XPathFactory.newInstance().newXPath();
            Node node = (Node) xpath.evaluate(localNameXPath, document, XPathConstants.NODE);
            if (node != null) {
                return node.getTextContent();
            }
        } catch (Exception e) {
            logger.debug("Failed to evaluate XPath with local-name(): {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Evaluate an XPath expression on a node and return the result as a string.
     * 
     * @param node The node to evaluate the XPath on
     * @param xpathExpression The XPath expression to evaluate
     * @return The result as a string, or null if no match
     * @throws Exception If evaluation fails
     */
    public String evaluateXPath(Node node, String xpathExpression) throws Exception {
        // First try with prefixes as-is
        try {
            XPath xpath = createXPath();
            Node resultNode = (Node) xpath.evaluate(xpathExpression, node, XPathConstants.NODE);
            if (resultNode != null) {
                return resultNode.getTextContent();
            }
        } catch (Exception e) {
            logger.debug("Failed to evaluate XPath with prefixes as-is: {}", e.getMessage());
        }
        
        // Try with prefix-trimmed XPath
        try {
            String trimmedXPath = trimPrefixes(xpathExpression);
            XPath xpath = createXPath();
            Node resultNode = (Node) xpath.evaluate(trimmedXPath, node, XPathConstants.NODE);
            if (resultNode != null) {
                return resultNode.getTextContent();
            }
        } catch (Exception e) {
            logger.debug("Failed to evaluate XPath with trimmed prefixes: {}", e.getMessage());
        }
        
        // Try with local-name() approach
        try {
            String localNameXPath = convertToLocalNameXPath(xpathExpression);
            XPath xpath = XPathFactory.newInstance().newXPath();
            Node resultNode = (Node) xpath.evaluate(localNameXPath, node, XPathConstants.NODE);
            if (resultNode != null) {
                return resultNode.getTextContent();
            }
        } catch (Exception e) {
            logger.debug("Failed to evaluate XPath with local-name(): {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Evaluate an XPath expression on a document and return the result as a node list.
     * 
     * @param document The document to evaluate the XPath on
     * @param xpathExpression The XPath expression to evaluate
     * @return The result as a NodeList, or null if no match
     * @throws Exception If evaluation fails
     */
    public NodeList evaluateXPathNodeList(Document document, String xpathExpression) throws Exception {
        // First try with prefixes as-is
        try {
            XPath xpath = createXPath();
            NodeList nodes = (NodeList) xpath.evaluate(xpathExpression, document, XPathConstants.NODESET);
            if (nodes != null && nodes.getLength() > 0) {
                return nodes;
            }
        } catch (Exception e) {
            logger.debug("Failed to evaluate XPath with prefixes as-is: {}", e.getMessage());
        }
        
        // Try with prefix-trimmed XPath
        try {
            String trimmedXPath = trimPrefixes(xpathExpression);
            XPath xpath = createXPath();
            NodeList nodes = (NodeList) xpath.evaluate(trimmedXPath, document, XPathConstants.NODESET);
            if (nodes != null && nodes.getLength() > 0) {
                return nodes;
            }
        } catch (Exception e) {
            logger.debug("Failed to evaluate XPath with trimmed prefixes: {}", e.getMessage());
        }
        
        // Try with local-name() approach
        try {
            String localNameXPath = convertToLocalNameXPath(xpathExpression);
            XPath xpath = XPathFactory.newInstance().newXPath();
            NodeList nodes = (NodeList) xpath.evaluate(localNameXPath, document, XPathConstants.NODESET);
            if (nodes != null && nodes.getLength() > 0) {
                return nodes;
            }
        } catch (Exception e) {
            logger.debug("Failed to evaluate XPath with local-name(): {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Evaluate an XPath expression on a document and return the result as a list of nodes.
     * 
     * @param document The document to evaluate the XPath on
     * @param xpathExpression The XPath expression to evaluate
     * @return The result as a List of Nodes, or empty list if no match
     * @throws Exception If evaluation fails
     */
    public List<Node> evaluateXPathToNodes(Document document, String xpathExpression) throws Exception {
        List<Node> result = new ArrayList<>();
        NodeList nodeList = evaluateXPathNodeList(document, xpathExpression);
        
        if (nodeList != null) {
            for (int i = 0; i < nodeList.getLength(); i++) {
                result.add(nodeList.item(i));
            }
        }
        
        return result;
    }
    
    /**
     * Get the value of an attribute from a node.
     * 
     * @param node The node to get the attribute from
     * @param attributeName The name of the attribute
     * @return The attribute value, or null if not found
     */
    public String getNodeAttributeValue(Node node, String attributeName) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element element = (Element) node;
            
            // Try direct attribute access
            if (element.hasAttribute(attributeName)) {
                return element.getAttribute(attributeName);
            }
            
            // Try with different case variations
            String camelCase = toCamelCase(attributeName);
            if (element.hasAttribute(camelCase)) {
                return element.getAttribute(camelCase);
            }
            
            // Try without underscores
            String noUnderscores = attributeName.replace("_", "");
            if (element.hasAttribute(noUnderscores)) {
                return element.getAttribute(noUnderscores);
            }
        }
        
        return null;
    }
    
    /**
     * Trim namespace prefixes from an XPath expression.
     * 
     * @param xpathExpression The XPath expression with prefixes
     * @return The XPath expression without prefixes
     */
    public String trimPrefixes(String xpathExpression) {
        if (xpathExpression == null || xpathExpression.isEmpty()) {
            return xpathExpression;
        }
        
        return PREFIX_PATTERN.matcher(xpathExpression).replaceAll("");
    }
    
    /**
     * Convert an XPath expression to use local-name() functions instead of prefixes.
     * 
     * @param xpathExpression The XPath expression with prefixes
     * @return The XPath expression using local-name() functions
     */
    public String convertToLocalNameXPath(String xpathExpression) {
        if (xpathExpression == null || xpathExpression.isEmpty()) {
            return xpathExpression;
        }
        
        // Split the XPath into steps
        String[] steps = xpathExpression.split("/");
        StringBuilder result = new StringBuilder();
        
        for (String step : steps) {
            if (step.isEmpty()) {
                result.append("/");
                continue;
            }
            
            // Check if this step has a prefix
            Matcher matcher = PREFIX_PATTERN.matcher(step);
            if (matcher.find()) {
                String prefix = matcher.group(1);
                String localName = step.substring(matcher.end());
                
                // Replace with local-name() function
                result.append("/*[local-name()='").append(localName).append("']");
            } else {
                result.append("/").append(step);
            }
        }
        
        // Handle the case where the XPath starts with //
        return xpathExpression.startsWith("//") 
            ? "/" + result.toString().replaceFirst("^/", "/") 
            : result.toString();
    }
    
    /**
     * Convert a snake_case string to camelCase.
     * 
     * @param snakeCase The snake_case string
     * @return The camelCase string
     */
    public String toCamelCase(String snakeCase) {
        if (snakeCase == null || snakeCase.isEmpty()) {
            return snakeCase;
        }
        
        StringBuilder camelCase = new StringBuilder();
        boolean capitalizeNext = false;
        
        for (char c : snakeCase.toCharArray()) {
            if (c == '_') {
                capitalizeNext = true;
            } else {
                camelCase.append(capitalizeNext ? Character.toUpperCase(c) : c);
                capitalizeNext = false;
            }
        }
        
        return camelCase.toString();
    }
    
    /**
     * Serialize a Document to a string.
     * 
     * @param document The Document to serialize
     * @return The serialized XML string
     * @throws Exception If serialization fails
     */
    public String serializeDocument(Document document) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        
        // Configure the transformer
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "0");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        
        // Transform the document to string
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        
        return writer.toString();
    }
    
    /**
     * Get the parent path from an XML path.
     * 
     * @param xmlPath The XML path
     * @return The parent path
     */
    public String getParentPath(String xmlPath) {
        if (xmlPath == null || xmlPath.isEmpty()) {
            return "";
        }
        
        int lastSlashIndex = xmlPath.lastIndexOf('/');
        if (lastSlashIndex > 0) {
            return xmlPath.substring(0, lastSlashIndex);
        }
        
        return xmlPath;
    }
    
    /**
     * Get the relative path by removing the parent path from the full path.
     * 
     * @param xmlPath The full XML path
     * @param parentPath The parent path to remove
     * @return The relative path
     */
    public String getRelativePath(String xmlPath, String parentPath) {
        if (xmlPath == null || xmlPath.isEmpty()) {
            return "";
        }
        
        if (parentPath == null || parentPath.isEmpty()) {
            return xmlPath;
        }
        
        if (xmlPath.startsWith(parentPath)) {
            String relativePath = xmlPath.substring(parentPath.length());
            if (relativePath.startsWith("/")) {
                relativePath = relativePath.substring(1);
            }
            return relativePath;
        }
        
        // If the paths don't match, try to extract the field name
        int lastSlashIndex = xmlPath.lastIndexOf('/');
        if (lastSlashIndex > 0) {
            return xmlPath.substring(lastSlashIndex + 1);
        }
        
        return xmlPath;
    }

    /**
     * Parse an XML file into a Document.
     *
     * @param file The XML file to parse
     * @return The parsed Document
     * @throws ValidationException if the file cannot be parsed
     */
    public Document parseXmlFile(MultipartFile file) {
        try {
            return parseXml(file.getInputStream());
        } catch (Exception e) {
            throw ValidationException.xmlParsingError("Failed to parse XML file", e);
        }
    }

    /**
     * Extract the root element name from an XML file.
     *
     * @param file The XML file
     * @return The name of the root element
     * @throws ValidationException if the file cannot be parsed
     */
    public String extractRootElement(MultipartFile file) {
        if (file == null) {
            throw ValidationException.missingRequiredField("file");
        }
        Document document = parseXmlFile(file);
        return document.getDocumentElement().getTagName();
    }

    /**
     * Extract the root element name from an XML string.
     *
     * @param xmlContent The XML content as string
     * @return The name of the root element
     * @throws ValidationException if the content cannot be parsed
     */
    public String extractRootElement(String xmlContent) {
        if (xmlContent == null || xmlContent.trim().isEmpty()) {
            throw ValidationException.missingRequiredField("xmlContent");
        }
        try {
            Document document = parseXml(xmlContent);
            return document.getDocumentElement().getTagName();
        } catch (Exception e) {
            throw ValidationException.xmlParsingError("Failed to parse XML content", e);
        }
    }

    /**
     * Create a MultipartFile from XML content.
     *
     * @param content The XML content as string
     * @param fileName The name of the file
     * @return A MultipartFile containing the XML content
     * @throws ValidationException if content or fileName is null or empty
     */
    public MultipartFile createMultipartFile(String content, String fileName) {
        if (content == null || content.trim().isEmpty()) {
            throw ValidationException.missingRequiredField("content");
        }
        if (fileName == null || fileName.trim().isEmpty()) {
            throw ValidationException.missingRequiredField("fileName");
        }
        return new MockMultipartFile(
            fileName,
            fileName,
            "application/xml",
            content.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Find or create an element at the specified path in the document.
     * If any element in the path doesn't exist, it will be created.
     *
     * @param document The XML document
     * @param path The path to the element (e.g., "root/parent/child")
     * @return The found or created element
     * @throws ValidationException if document or path is null or empty
     */
    public Element findOrCreateElement(Document document, String path) {
        if (document == null) {
            throw ValidationException.missingRequiredField("document");
        }
        if (path == null || path.trim().isEmpty()) {
            throw ValidationException.missingRequiredField("path");
        }

        String[] parts = path.split("/");
        Element current = document.getDocumentElement();
        
        for (String part : parts) {
            if (part.trim().isEmpty()) {
                throw ValidationException.schemaValidationError("Invalid empty element name in path: " + path);
            }
            NodeList children = current.getElementsByTagName(part);
            if (children.getLength() > 0) {
                current = (Element) children.item(0);
            } else {
                Element newElement = document.createElement(part);
                current.appendChild(newElement);
                current = newElement;
                logger.debug("Created new element: {}", part);
            }
        }
        
        return current;
    }
}
