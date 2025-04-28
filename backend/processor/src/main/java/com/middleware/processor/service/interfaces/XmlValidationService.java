package com.middleware.processor.service.interfaces;

import org.w3c.dom.Document;
import com.middleware.shared.model.Interface;

/**
 * Service interface for XML validation operations.
 * Enhanced to support all XML types and proper schema matching.
 */
public interface XmlValidationService {
    /**
     * Validates an XML document against an XSD schema.
     * 
     * @param document The XML document to validate
     * @param xsdContent The XSD schema content as a string
     * @return true if validation succeeds, false otherwise
     */
    boolean validateXmlAgainstXsd(Document document, String xsdContent);

    /**
     * Validates the structure of an XML document.
     * 
     * @param document The XML document to validate
     * @return true if validation succeeds, false otherwise
     */
    boolean validateXmlStructure(Document document);

    /**
     * Validates the content of an XML document against business rules.
     * @deprecated Use {@link #validateXmlContent(Document, Interface)} instead
     */
    @Deprecated
    boolean validateXmlContent(Document document, String interfaceType);

    /**
     * Validates the content of an XML document against the interface's XSD schema.
     * 
     * @param document The XML document to validate
     * @param interfaceEntity The interface containing the schema path
     * @return true if validation succeeds, false otherwise
     */
    boolean validateXmlContent(Document document, Interface interfaceEntity);

    /**
     * Gets the validation error message from the last validation operation.
     * 
     * @return The validation error message, or null if no error occurred
     */
    String getValidationErrorMessage();

    /**
     * Validates XML content provided as a string.
     * 
     * @param xmlContent The XML content as a string
     * @param interfaceType The type of interface to validate against
     * @throws Exception if validation fails
     */
    void validateXmlContent(String xmlContent, String interfaceType) throws Exception;
    
    /**
     * Performs a preliminary check to determine if an XML document matches an XSD schema.
     * This checks root element name and namespace compatibility before full validation.
     * 
     * @param document The XML document to check
     * @param xsdContent The XSD schema content as a string
     * @return true if the document potentially matches the schema, false otherwise
     */
    boolean isXmlCompatibleWithXsd(Document document, String xsdContent);
    
    /**
     * Performs a preliminary check to determine if an XML document matches an interface's XSD schema.
     * This checks root element name and namespace compatibility before full validation.
     * 
     * @param document The XML document to check
     * @param interfaceEntity The interface containing the schema path
     * @return true if the document potentially matches the schema, false otherwise
     */
    boolean isXmlCompatibleWithInterface(Document document, Interface interfaceEntity);
    
    /**
     * Extracts the root element name and namespace from an XSD schema.
     * 
     * @param xsdContent The XSD schema content as a string
     * @return A map containing the root element name and namespace
     */
    java.util.Map<String, String> extractXsdRootElementInfo(String xsdContent);
    
    /**
     * Validates an XML document against an XSD schema with strict namespace checking.
     * 
     * @param document The XML document to validate
     * @param xsdContent The XSD schema content as a string
     * @param enforceNamespaces Whether to enforce namespace matching
     * @return true if validation succeeds, false otherwise
     */
    boolean validateXmlAgainstXsdWithNamespaceCheck(Document document, String xsdContent, boolean enforceNamespaces);
}
