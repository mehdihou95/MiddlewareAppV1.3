package com.middleware.processor.service.util;

import com.middleware.processor.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.util.*;

/**
 * Enhanced XML processing utility class.
 * Provides centralized XML handling with improved namespace support.
 */
@Component
public class XmlProcessor {

    private static final Logger log = LoggerFactory.getLogger(XmlProcessor.class);

    /**
     * Parse an XML file into a Document object.
     *
     * @param file The MultipartFile containing XML content
     * @return The parsed Document
     * @throws Exception If parsing fails
     */
    public Document parseXmlFile(MultipartFile file) throws Exception {
        try {
            byte[] bytes = file.getBytes();
            return parseXmlBytes(bytes);
        } catch (Exception e) {
            log.error("Failed to parse XML file: {}", e.getMessage());
            throw new ValidationException("Invalid XML file: " + e.getMessage(), e);
        }
    }

    /**
     * Parse XML content from a byte array.
     *
     * @param bytes The XML content as bytes
     * @return The parsed Document
     * @throws Exception If parsing fails
     */
    public Document parseXmlBytes(byte[] bytes) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true); // Enable namespace support
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); // Security: Disallow DTDs
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false); // Security: Disable external entities
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false); // Security: Disable external parameter entities
        factory.setXIncludeAware(false); // Security: Disable XInclude
        factory.setExpandEntityReferences(false); // Security: Don't expand entity references

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new ByteArrayInputStream(bytes));
        
        // Extract and register all namespaces
        Map<String, String> namespaces = extractNamespaces(document.getDocumentElement());
        log.debug("Extracted {} namespaces from document", namespaces.size());
        
        return document;
    }

    /**
     * Parse XML content from a string.
     *
     * @param xmlString The XML content as a string
     * @return The parsed Document
     * @throws Exception If parsing fails
     */
    public Document parseXmlString(String xmlString) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true); // Enable namespace support
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); // Security: Disallow DTDs
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false); // Security: Disable external entities
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false); // Security: Disable external parameter entities
        factory.setXIncludeAware(false); // Security: Disable XInclude
        factory.setExpandEntityReferences(false); // Security: Don't expand entity references

        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource inputSource = new InputSource(new StringReader(xmlString));
        Document document = builder.parse(inputSource);
        
        // Extract and register all namespaces
        Map<String, String> namespaces = extractNamespaces(document.getDocumentElement());
        log.debug("Extracted {} namespaces from document", namespaces.size());
        
        return document;
    }

    /**
     * Extract the root element name from an XML file.
     *
     * @param file The MultipartFile containing XML content
     * @return The root element name
     * @throws Exception If extraction fails
     */
    public String extractRootElement(MultipartFile file) throws Exception {
        Document document = parseXmlFile(file);
        Element rootElement = document.getDocumentElement();
        return rootElement.getNodeName();
    }

    /**
     * Serialize a Document to a string.
     *
     * @param document The Document to serialize
     * @return The serialized XML as a string
     * @throws Exception If serialization fails
     */
    public String serializeDocument(Document document) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return writer.toString();
    }

    /**
     * Serialize a Document to a byte array.
     *
     * @param document The Document to serialize
     * @return The serialized XML as bytes
     * @throws Exception If serialization fails
     */
    public byte[] serializeDocumentToBytes(Document document) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(document), new StreamResult(outputStream));
        return outputStream.toByteArray();
    }

    /**
     * Create a MultipartFile from XML content.
     *
     * @param content The XML content as a string
     * @param filename The filename to use
     * @return A MultipartFile containing the XML content
     * @throws Exception If creation fails
     */
    public MultipartFile createMultipartFile(String content, String filename) throws Exception {
        byte[] bytes = content.getBytes();
        return new ByteArrayMultipartFile(bytes, filename);
    }

    /**
     * Create a MultipartFile from XML content.
     *
     * @param bytes The XML content as bytes
     * @param filename The filename to use
     * @return A MultipartFile containing the XML content
     */
    public MultipartFile createMultipartFile(byte[] bytes, String filename) {
        return new ByteArrayMultipartFile(bytes, filename);
    }

    /**
     * Evaluate an XPath expression on a Document and return the result as a string.
     *
     * @param document The Document to evaluate against
     * @param xpathExpression The XPath expression
     * @return The result as a string, or null if not found
     * @throws Exception If evaluation fails
     */
    public String evaluateXPath(Document document, String xpathExpression) throws Exception {
        Map<String, String> namespaces = extractNamespaces(document.getDocumentElement());
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xpath = xPathFactory.newXPath();
        
        // Set namespace context if namespaces are present
        if (!namespaces.isEmpty()) {
            xpath.setNamespaceContext(new MapNamespaceContext(namespaces));
        }
        
        XPathExpression expr = xpath.compile(xpathExpression);
        Object result = expr.evaluate(document, XPathConstants.STRING);
        return result != null ? result.toString() : null;
    }

    /**
     * Evaluate an XPath expression on an Element and return the result as a string.
     *
     * @param element The Element to evaluate against
     * @param xpathExpression The XPath expression
     * @return The result as a string, or null if not found
     * @throws Exception If evaluation fails
     */
    public String evaluateXPath(Element element, String xpathExpression) throws Exception {
        Map<String, String> namespaces = extractNamespaces(element);
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xpath = xPathFactory.newXPath();
        
        // Set namespace context if namespaces are present
        if (!namespaces.isEmpty()) {
            xpath.setNamespaceContext(new MapNamespaceContext(namespaces));
        }
        
        XPathExpression expr = xpath.compile(xpathExpression);
        Object result = expr.evaluate(element, XPathConstants.STRING);
        return result != null ? result.toString() : null;
    }

    /**
     * Evaluate an XPath expression on a Document and return the result as a NodeList.
     *
     * @param document The Document to evaluate against
     * @param xpathExpression The XPath expression
     * @return The result as a NodeList
     * @throws Exception If evaluation fails
     */
    public NodeList evaluateXPathForNodes(Document document, String xpathExpression) throws Exception {
        Map<String, String> namespaces = extractNamespaces(document.getDocumentElement());
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xpath = xPathFactory.newXPath();
        
        // Set namespace context if namespaces are present
        if (!namespaces.isEmpty()) {
            xpath.setNamespaceContext(new MapNamespaceContext(namespaces));
        }
        
        XPathExpression expr = xpath.compile(xpathExpression);
        return (NodeList) expr.evaluate(document, XPathConstants.NODESET);
    }

    /**
     * Extract all namespace declarations from an Element and its children.
     *
     * @param element The Element to extract namespaces from
     * @return A map of namespace prefixes to URIs
     */
    public Map<String, String> extractNamespaces(Element element) {
        Map<String, String> namespaces = new HashMap<>();
        
        // Extract namespaces from the element itself
        extractNamespacesFromElement(element, namespaces);
        
        // Recursively extract namespaces from child elements
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                extractNamespacesFromElement((Element) child, namespaces);
            }
        }
        
        return namespaces;
    }

    /**
     * Extract namespace declarations from a single Element.
     *
     * @param element The Element to extract namespaces from
     * @param namespaces The map to add namespaces to
     */
    private void extractNamespacesFromElement(Element element, Map<String, String> namespaces) {
        // Extract namespace URI for the element's own namespace
        String namespaceURI = element.getNamespaceURI();
        if (namespaceURI != null && !namespaceURI.isEmpty()) {
            String prefix = element.getPrefix();
            prefix = prefix != null ? prefix : ""; // Default namespace has empty prefix
            namespaces.put(prefix, namespaceURI);
        }
        
        // Extract all xmlns:* attributes
        if (element.hasAttributes()) {
            for (int i = 0; i < element.getAttributes().getLength(); i++) {
                Node attr = element.getAttributes().item(i);
                String attrName = attr.getNodeName();
                
                // Check for xmlns or xmlns:prefix attributes
                if (attrName.equals("xmlns") || attrName.startsWith("xmlns:")) {
                    String prefix = attrName.equals("xmlns") ? "" : attrName.substring(6); // Remove "xmlns:"
                    String uri = attr.getNodeValue();
                    namespaces.put(prefix, uri);
                }
            }
        }
    }

    /**
     * Implementation of NamespaceContext that uses a Map for namespace lookups.
     */
    private static class MapNamespaceContext implements NamespaceContext {
        private final Map<String, String> prefixToUri;
        private final Map<String, String> uriToPrefix;
        
        public MapNamespaceContext(Map<String, String> namespaces) {
            this.prefixToUri = new HashMap<>(namespaces);
            this.uriToPrefix = new HashMap<>();
            
            // Create reverse mapping for getPrefix
            for (Map.Entry<String, String> entry : namespaces.entrySet()) {
                // Only store the first prefix for each URI
                if (!uriToPrefix.containsKey(entry.getValue())) {
                    uriToPrefix.put(entry.getValue(), entry.getKey());
                }
            }
        }
        
        @Override
        public String getNamespaceURI(String prefix) {
            if (prefix == null) {
                throw new IllegalArgumentException("Null prefix");
            }
            if (prefix.equals(XMLConstants.XML_NS_PREFIX)) {
                return XMLConstants.XML_NS_URI;
            }
            if (prefix.equals(XMLConstants.XMLNS_ATTRIBUTE)) {
                return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
            }
            String uri = prefixToUri.get(prefix);
            return uri != null ? uri : XMLConstants.NULL_NS_URI;
        }
        
        @Override
        public String getPrefix(String namespaceURI) {
            if (namespaceURI == null) {
                throw new IllegalArgumentException("Null namespace URI");
            }
            if (namespaceURI.equals(XMLConstants.XML_NS_URI)) {
                return XMLConstants.XML_NS_PREFIX;
            }
            if (namespaceURI.equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI)) {
                return XMLConstants.XMLNS_ATTRIBUTE;
            }
            return uriToPrefix.get(namespaceURI);
        }
        
        @Override
        public Iterator<String> getPrefixes(String namespaceURI) {
            if (namespaceURI == null) {
                throw new IllegalArgumentException("Null namespace URI");
            }
            if (namespaceURI.equals(XMLConstants.XML_NS_URI)) {
                return Collections.singletonList(XMLConstants.XML_NS_PREFIX).iterator();
            }
            if (namespaceURI.equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI)) {
                return Collections.singletonList(XMLConstants.XMLNS_ATTRIBUTE).iterator();
            }
            
            // Find all prefixes for this URI
            List<String> prefixes = new ArrayList<>();
            for (Map.Entry<String, String> entry : prefixToUri.entrySet()) {
                if (namespaceURI.equals(entry.getValue())) {
                    prefixes.add(entry.getKey());
                }
            }
            return prefixes.iterator();
        }
    }

    /**
     * Implementation of MultipartFile that wraps a byte array.
     */
    private static class ByteArrayMultipartFile implements MultipartFile {
        private final byte[] content;
        private final String filename;
        
        public ByteArrayMultipartFile(byte[] content, String filename) {
            this.content = content;
            this.filename = filename;
        }
        
        @Override
        public String getName() {
            return filename;
        }
        
        @Override
        public String getOriginalFilename() {
            return filename;
        }
        
        @Override
        public String getContentType() {
            return "application/xml";
        }
        
        @Override
        public boolean isEmpty() {
            return content == null || content.length == 0;
        }
        
        @Override
        public long getSize() {
            return content.length;
        }
        
        @Override
        public byte[] getBytes() throws IOException {
            return content;
        }
        
        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(content);
        }
        
        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException {
            try (FileOutputStream output = new FileOutputStream(dest)) {
                output.write(content);
            }
        }
    }

    /**
     * XML constants for namespace handling.
     */
    private static class XMLConstants {
        public static final String XML_NS_PREFIX = "xml";
        public static final String XML_NS_URI = "http://www.w3.org/XML/1998/namespace";
        public static final String XMLNS_ATTRIBUTE = "xmlns";
        public static final String XMLNS_ATTRIBUTE_NS_URI = "http://www.w3.org/2000/xmlns/";
        public static final String NULL_NS_URI = "";
    }
}
