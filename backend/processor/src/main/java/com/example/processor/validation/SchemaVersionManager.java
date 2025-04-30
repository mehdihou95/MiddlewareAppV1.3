package com.example.processor.validation;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SchemaVersionManager {

    private final Map<String, Schema> schemaCache = new ConcurrentHashMap<>();
    private final DocumentBuilderFactory documentBuilderFactory;

    public SchemaVersionManager() {
        documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        documentBuilderFactory.setValidating(false);
    }

    public Schema getSchema(String version) throws SAXException {
        return schemaCache.computeIfAbsent(version, this::loadSchema);
    }

    private Schema loadSchema(String version) {
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            
            String schemaPath = String.format("/schemas/v%s/schema.xsd", version);
            try (InputStream schemaStream = getClass().getResourceAsStream(schemaPath)) {
                if (schemaStream == null) {
                    throw new IllegalArgumentException("Schema not found for version: " + version);
                }
                return factory.newSchema(new javax.xml.transform.stream.StreamSource(schemaStream));
            }
        } catch (SAXException | IOException e) {
            throw new RuntimeException("Failed to load schema for version: " + version, e);
        }
    }

    public Document parseDocument(InputStream xmlStream) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
        return builder.parse(xmlStream);
    }

    public void clearCache() {
        schemaCache.clear();
    }
} 