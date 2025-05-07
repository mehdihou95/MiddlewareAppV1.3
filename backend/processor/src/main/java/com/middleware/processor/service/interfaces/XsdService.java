package com.middleware.processor.service.interfaces;

import com.middleware.shared.model.MappingRule;
import com.middleware.shared.model.Interface;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

/**
 * Service interface for XSD schema operations.
 * Enhanced to support all XML types including namespaces, mixed content, attributes, and processing instructions.
 */
public interface XsdService {
    /**
     * Validate an XSD schema file.
     *
     * @param file The XSD file to validate
     * @return true if the schema is valid, false otherwise
     */
    boolean validateXsdSchema(MultipartFile file);

    /**
     * Process an XSD schema file and update the interface.
     *
     * @param file The XSD file to process
     * @param interfaceEntity The interface to update
     * @return The updated interface
     */
    Interface processXsdSchema(MultipartFile file, Interface interfaceEntity);

    /**
     * Get the root element from an XSD schema.
     *
     * @param file The XSD file to analyze
     * @return The root element name
     */
    String getRootElement(MultipartFile file);

    /**
     * Get the namespace from an XSD schema.
     *
     * @param file The XSD file to analyze
     * @return The namespace URI
     */
    String getNamespace(MultipartFile file);

    /**
     * Get all namespaces from an XSD schema.
     *
     * @param file The XSD file to analyze
     * @return List of namespace prefix and URI pairs
     */
    List<Map<String, String>> getAllNamespaces(MultipartFile file);

    /**
     * Get the structure of an XSD schema.
     *
     * @param xsdPath The path to the XSD schema
     * @return List of schema elements and their properties
     */
    List<Map<String, Object>> getXsdStructure(String xsdPath);

    /**
     * Get the structure of an XSD schema for a specific client.
     *
     * @param xsdPath The path to the XSD schema
     * @param clientId The ID of the client
     * @return List of schema elements and their properties
     */
    List<Map<String, Object>> getXsdStructure(String xsdPath, Long clientId);

    /**
     * Get all mapping rules with pagination.
     *
     * @param pageable The pagination information
     * @return Page of mapping rules
     */
    Page<MappingRule> getAllMappingRules(Pageable pageable);

    /**
     * Get mapping rules for a specific client with pagination.
     *
     * @param clientId The ID of the client
     * @param pageable The pagination information
     * @return Page of mapping rules
     */
    Page<MappingRule> getMappingRulesByClient(Long clientId, Pageable pageable);

    /**
     * Get mapping rules for a specific interface with pagination.
     *
     * @param interfaceId The ID of the interface
     * @param pageable The pagination information
     * @return Page of mapping rules
     */
    Page<MappingRule> getMappingRulesByInterface(Long interfaceId, Pageable pageable);

    /**
     * Get mapping rules for a specific client and interface with pagination.
     *
     * @param clientId The ID of the client
     * @param interfaceId The ID of the interface
     * @param pageable The pagination information
     * @return Page of mapping rules
     */
    Page<MappingRule> getMappingRulesByClientAndInterface(Long clientId, Long interfaceId, Pageable pageable);

    /**
     * Delete a mapping rule by ID.
     *
     * @param id The ID of the rule to delete
     */
    void deleteMappingRule(Long id);

    /**
     * Get active mapping rules for an interface.
     *
     * @param interfaceId The ID of the interface
     * @return List of active mapping rules
     */
    List<MappingRule> getActiveMappingRules(Long interfaceId);

    /**
     * Get mapping rules by table name and client ID.
     *
     * @param tableName The name of the table
     * @param clientId The ID of the client
     * @return List of mapping rules
     */
    List<MappingRule> findByTableNameAndClient_Id(String tableName, Long clientId);

    /**
     * Delete mapping rules by client ID and table name.
     *
     * @param clientId The ID of the client
     * @param tableName The name of the table
     */
    void deleteByClient_IdAndTableName(Long clientId, String tableName);

    /**
     * Get XSD structure.
     * 
     * @param xsdFile The XSD file to analyze
     * @return JSON string representation of the XSD structure
     */
    String analyzeXsdStructure(MultipartFile xsdFile);

    /**
     * Get XSD structure with client context.
     * 
     * @param xsdFile The XSD file to analyze
     * @param clientId The ID of the client
     * @return JSON string representation of the XSD structure with client context
     */
    String analyzeXsdStructureWithClient(MultipartFile xsdFile, Long clientId);

    /**
     * Get mapping rules for a client.
     *
     * @param clientId The ID of the client
     * @return List of mapping rules
     */
    List<MappingRule> getMappingRules(Long clientId);

    /**
     * Get active mapping rules for a client with pagination.
     *
     * @param clientId The ID of the client
     * @param pageable The pagination information
     * @return List of active mapping rules
     */
    List<MappingRule> getActiveMappingRules(Long clientId, Pageable pageable);

    /**
     * Get mapping rules by table name.
     *
     * @param tableName The name of the table
     * @return List of mapping rules
     */
    List<MappingRule> getMappingRulesByTableName(String tableName);

    /**
     * Delete mapping rules by client and table.
     *
     * @param clientId The ID of the client
     * @param tableName The name of the table
     */
    void deleteMappingRulesByClientAndTable(Long clientId, String tableName);
    
    /**
     * Get all namespaces from an XSD schema by interface ID.
     *
     * @param interfaceId The ID of the interface
     * @return List of namespace prefix and URI pairs
     */
    List<Map<String, String>> getXsdNamespaces(Long interfaceId);
    
    /**
     * Analyze an XSD file and extract its structure, namespaces, and other metadata.
     *
     * @param file The XSD file to analyze
     * @return Map containing the root element, namespaces, and structure
     */
    Map<String, Object> analyzeXsdFile(MultipartFile file);
    
    /**
     * Process complex types in an XSD schema.
     *
     * @param xsdPath The path to the XSD schema
     * @return Map of complex type names to their structures
     */
    Map<String, Map<String, Object>> getComplexTypes(String xsdPath);
    
    /**
     * Get attributes defined in an XSD schema.
     *
     * @param xsdPath The path to the XSD schema
     * @return List of attribute definitions
     */
    List<Map<String, Object>> getXsdAttributes(String xsdPath);
    
    /**
     * Check if an XSD schema contains mixed content models.
     *
     * @param xsdPath The path to the XSD schema
     * @return true if the schema contains mixed content models, false otherwise
     */
    boolean hasMixedContent(String xsdPath);
    
    /**
     * Get processing instructions from an XSD schema.
     *
     * @param xsdPath The path to the XSD schema
     * @return List of processing instructions
     */
    List<Map<String, String>> getProcessingInstructions(String xsdPath);
}
