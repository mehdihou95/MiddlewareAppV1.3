package com.middleware.processor.service.strategy;

import com.middleware.processor.exception.ValidationException;
import com.middleware.processor.service.interfaces.XmlValidationService;
import com.middleware.processor.service.util.TransformationService;
import com.middleware.processor.service.util.XmlProcessor;
import com.middleware.shared.model.Client;
import com.middleware.shared.model.Interface;
import com.middleware.shared.model.MappingRule;
import com.middleware.shared.model.ProcessedFile;
import com.middleware.shared.model.TargetLevel;
import com.middleware.shared.repository.MappingRuleRepository;
import com.middleware.shared.repository.ProcessedFileRepository;
import com.middleware.shared.service.util.CircuitBreakerService;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Abstract template for document processing strategies.
 * Provides common functionality for validation, transformation, and persistence.
 * Standardized transaction management with REQUIRED propagation for the main processDocument method.
 */
public abstract class BaseDocumentProcessingTemplate extends BaseDocumentProcessingStrategy {

    private static final Logger log = LoggerFactory.getLogger(BaseDocumentProcessingTemplate.class);

    @Autowired
    protected XmlValidationService xmlValidationService;

    @Autowired
    protected MappingRuleRepository mappingRuleRepository;

    @Autowired
    protected ProcessedFileRepository processedFileRepository;

    @Autowired
    protected TransformationService transformationService;

    @Autowired
    protected XmlProcessor xmlProcessor;

    @Autowired
    protected CircuitBreakerService circuitBreakerService;

    /**
     * Main processing method that orchestrates the document processing flow.
     * Runs within the transaction initiated by the calling service (e.g., XmlProcessorServiceImpl).
     *
     * @param file            The multipart file containing the XML document.
     * @param interfaceEntity The interface configuration for this document.
     * @return The ProcessedFile entity representing the result.
     */
    @Transactional(propagation = Propagation.REQUIRED) // Ensures participation in the outer transaction
    public ProcessedFile processDocument(MultipartFile file, Interface interfaceEntity) {
        log.debug("Processing document {} for interface {}", file.getOriginalFilename(), interfaceEntity.getName());

        ProcessedFile processedFile = new ProcessedFile();
        processedFile.setFileName(file.getOriginalFilename());
        processedFile.setStatus("PROCESSING");
        processedFile.setInterfaceEntity(interfaceEntity);
        processedFile.setClient(interfaceEntity.getClient()); // Use client from interface initially
        processedFile.setProcessedAt(LocalDateTime.now());

        try {
            // Save initial processing status
            // Note: Saving here might cause issues if the outer transaction rolls back.
            // Consider saving the ProcessedFile record only at the end or in the calling service.
            processedFile = processedFileRepository.save(processedFile);

            // Parse XML document
            Document document = xmlProcessor.parseXmlFile(file);

            // Validate XML
            validateXml(document, interfaceEntity);

            // Process document using the specific strategy implementation
            Object processedEntity = processSpecificDocument(document, interfaceEntity);

            // Update processed file status to SUCCESS
            processedFile.setStatus("SUCCESS");
            processedFile.setContent(xmlProcessor.serializeDocument(document)); // Store processed content if needed
            return processedFileRepository.save(processedFile);

        } catch (ValidationException e) {
            log.error("Validation error processing document {}: {}", file.getOriginalFilename(), e.getMessage());
            processedFile.setStatus("ERROR");
            processedFile.setErrorMessage("Validation error: " + e.getMessage());
            // Ensure the error record is saved even if initial save happened
            return processedFileRepository.save(processedFile);
        } catch (Exception e) {
            log.error("Error processing document {}: {}", file.getOriginalFilename(), e.getMessage(), e);
            processedFile.setStatus("ERROR");
            processedFile.setErrorMessage("Processing error: " + e.getMessage());
            // Ensure the error record is saved even if initial save happened
            return processedFileRepository.save(processedFile);
        }
    }

    /**
     * Validates an XML document against its schema and performs additional validations.
     * Runs within the existing transaction.
     *
     * @param document        The parsed XML document.
     * @param interfaceEntity The interface configuration.
     * @throws ValidationException If validation fails.
     */
    @Transactional(propagation = Propagation.MANDATORY) // Must be called within an existing transaction
    protected void validateXml(Document document, Interface interfaceEntity) throws ValidationException {
        if (interfaceEntity.getSchemaPath() != null && !interfaceEntity.getSchemaPath().isEmpty()) {
            boolean isValid = xmlValidationService.validateXmlContent(document, interfaceEntity);
            if (!isValid) {
                throw new ValidationException("XML validation failed against schema: " + interfaceEntity.getSchemaPath() +
                        ". Error: " + xmlValidationService.getValidationErrorMessage());
            }
        } else {
            log.warn("No XSD schema path defined for interface {}, skipping schema validation.", interfaceEntity.getName());
        }
        // Add further structural or business rule validations if needed
        performAdditionalValidations(document, interfaceEntity);
    }

    /**
     * Placeholder for additional validation logic specific to the document type.
     * To be implemented by subclasses if needed.
     *
     * @param document        The parsed XML document.
     * @param interfaceEntity The interface configuration.
     * @throws ValidationException If additional validation fails.
     */
    protected void performAdditionalValidations(Document document, Interface interfaceEntity) throws ValidationException {
        // Default implementation does nothing
    }

    /**
     * Abstract method for specific document processing logic.
     * Must be implemented by concrete strategy classes.
     * Recommended propagation is REQUIRED to participate in the main transaction.
     *
     * @param document        The parsed XML document.
     * @param interfaceEntity The interface configuration.
     * @return The processed domain entity (e.g., AsnHeader, OrderHeader).
     * @throws Exception If processing fails.
     */
    // @Transactional annotation should be on the implementing method in the concrete class
    protected abstract Object processSpecificDocument(Document document, Interface interfaceEntity) throws Exception;

    /**
     * Retrieves active mapping rules for a given interface.
     * Runs within the existing transaction.
     *
     * @param interfaceId The ID of the interface.
     * @return A list of active mapping rules.
     */
    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    protected List<MappingRule> getActiveMappingRules(Long interfaceId) {
        return mappingRuleRepository.findByInterfaceIdAndIsActiveTrue(interfaceId);
    }

    // --- Standardized Field Processing --- 

    /**
     * Applies a mapping rule to set a field on an entity, handling transformation and default values.
     *
     * @param entity     The target entity.
     * @param rule       The mapping rule to apply.
     * @param rawXmlValue The raw value extracted from the XML (can be null or empty).
     * @throws Exception If a required field is missing or transformation/setting fails.
     */
    protected void applyRuleToField(Object entity, MappingRule rule, String rawXmlValue) throws Exception {
        String targetFieldName = rule.getTargetField();
        if (targetFieldName == null || targetFieldName.trim().isEmpty()) {
            log.warn("Skipping rule 	{}	 because target field is not defined.", rule.getName());
            return;
        }

        try {
            Class<?> targetType = getTargetFieldType(entity, targetFieldName);
            boolean isNullable = isFieldNullable(entity, targetFieldName);
            Object finalValue = null;
            String sourceValue = null; // To store the value used (XML or Default)

            if (rawXmlValue != null && !rawXmlValue.trim().isEmpty()) {
                // Use TransformationService for XML value
                finalValue = transformationService.transformAndConvert(
                    rawXmlValue,
                    rule.getTransformation(),
                    targetType
                );
                sourceValue = "XML";
                log.debug("Transformed XML value for {}: {}", targetFieldName, finalValue);
            } else if (rule.getDefaultValue() != null && !rule.getDefaultValue().trim().isEmpty()) {
                // Use TransformationService for Default value if XML value is empty/null
                finalValue = transformationService.transformAndConvert(
                    rule.getDefaultValue(),
                    rule.getTransformation(), // Apply transformation to default value too
                    targetType
                );
                sourceValue = "Default";
                log.debug("Using transformed default value for {}: {}", targetFieldName, finalValue);
            }

            // Check requirement only after attempting to get a value
            if (finalValue == null && rule.getRequired()) {
                 throw new ValidationException("Required field '" + targetFieldName + "' is missing or resulted in null value (Rule: " + rule.getName() + ").");
            } else if (finalValue == null && !isNullable) {
                 // If field is non-nullable in DB and value is null, throw error
                 log.error("Non-nullable field 	{}	 resulted in null value from {} source. Check mapping or source data (Rule: {}).", targetFieldName, sourceValue, rule.getName());
                 throw new ValidationException("Non-nullable field '" + targetFieldName + "' cannot be null (Rule: " + rule.getName() + ").");
            }

            // Set the field value using reflection (or a safer method)
            setEntityField(entity, targetFieldName, finalValue);

        } catch (Exception e) {
            log.error("Failed to process rule 	{}	 for field 	{}	: {}", rule.getName(), targetFieldName, e.getMessage());
            if (rule.getRequired()) {
                // Re-throw exception if the field was required
                throw new ValidationException("Error processing required field " + targetFieldName + " (Rule: " + rule.getName() + "): " + e.getMessage(), e);
            }
            // For non-required fields, log the error but continue processing other fields
            log.warn("Skipping non-required field 	{}	 due to processing error (Rule: {}).", targetFieldName, rule.getName());
        }
    }

    /** Helper method to get target field type using reflection. */
    protected Class<?> getTargetFieldType(Object entity, String fieldName) throws NoSuchFieldException {
        Field field = findField(entity.getClass(), fieldName);
        return field.getType();
    }

    /** Helper method to check field nullability based on annotations. */
    protected boolean isFieldNullable(Object entity, String fieldName) {
        try {
            Field field = findField(entity.getClass(), fieldName);
            // Check for @NotNull annotation
            if (field.isAnnotationPresent(NotNull.class)) {
                return false;
            }
            // Check for @Column(nullable = false)
            if (field.isAnnotationPresent(Column.class)) {
                Column column = field.getAnnotation(Column.class);
                if (!column.nullable()) {
                    return false;
                }
            }
            return true; // Default to nullable if no constraints found
        } catch (NoSuchFieldException e) {
            log.warn("Could not determine nullability for field {}, assuming nullable", fieldName);
            return true;
        }
    }

    /** Helper method to set entity field value using reflection. */
    protected void setEntityField(Object entity, String fieldName, Object value) throws Exception {
        try {
            Field field = findField(entity.getClass(), fieldName);
            field.setAccessible(true);
            // Basic type coercion might be needed if transformAndConvert doesn't guarantee exact type match
            // For example, handle conversion between Integer and Long if necessary
            Object convertedValue = value;
            if (value != null && !field.getType().isAssignableFrom(value.getClass())) {
                // Attempt basic conversions or rely on framework/DB conversion
                log.warn("Potential type mismatch for field {}. Expected {}, got {}. Attempting to set anyway.",
                         fieldName, field.getType().getSimpleName(), value.getClass().getSimpleName());
                // Add specific conversion logic here if needed
            }
            field.set(entity, convertedValue);
            log.debug("Set field 	{}	 to value: {}", fieldName, convertedValue);
        } catch (NoSuchFieldException e) {
            log.error("Field 	{}	 not found in entity class {}", fieldName, entity.getClass().getSimpleName());
            throw e;
        } catch (IllegalAccessException e) {
            log.error("Cannot access field 	{}	 in entity class {}", fieldName, entity.getClass().getSimpleName());
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("Type mismatch setting field 	{}	 with value 	{}	 (type {}). Expected type {}. Error: {}",
                      fieldName, value, (value != null ? value.getClass().getSimpleName() : "null"),
                      findField(entity.getClass(), fieldName).getType().getSimpleName(), e.getMessage());
            // Provide more context in the exception
            throw new IllegalArgumentException("Type mismatch for field '" + fieldName + "'. Expected " +
                                             findField(entity.getClass(), fieldName).getType().getSimpleName() +
                                             " but got " + (value != null ? value.getClass().getSimpleName() : "null"), e);
        }
    }

    /** Helper to find field, handling potential naming conventions (snake_case to camelCase) and inheritance. */
    private Field findField(Class<?> entityClass, String dbFieldName) throws NoSuchFieldException {
        // Convert database field name (snake_case) to Java field name (camelCase)
        String fieldName = convertDbNameToCamelCase(dbFieldName);

        Class<?> currentClass = entityClass;
        while (currentClass != null) {
            try {
                return currentClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                // Field not found in current class, try superclass
                currentClass = currentClass.getSuperclass();
            }
        }
        // Field not found in the entire hierarchy
        throw new NoSuchFieldException("Field '" + fieldName + "' (derived from '" + dbFieldName + "') not found in class " + entityClass.getName() + " or its superclasses.");
    }

    /** Converts snake_case or simple names to camelCase. */
    private String convertDbNameToCamelCase(String dbName) {
        if (dbName == null || dbName.isEmpty()) {
            return dbName;
        }
        // If no underscore, assume it's already camelCase or a simple name
        if (!dbName.contains("_")) {
             // Handle potential all-caps names from DB
             if (dbName.toUpperCase().equals(dbName)) {
                 return dbName.toLowerCase();
             }
             // Assume it's correct or simple name
             return dbName;
        }

        StringBuilder camelCaseName = new StringBuilder();
        boolean capitalizeNext = false;
        for (char c : dbName.toLowerCase().toCharArray()) {
            if (c == '_') {
                capitalizeNext = true;
            } else {
                if (capitalizeNext) {
                    camelCaseName.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    camelCaseName.append(c);
                }
            }
        }
        return camelCaseName.toString();
    }

    // --- Standardized Line Processing (Conceptual) ---

    /**
     * Processes line items from the XML document based on mapping rules.
     * This is a conceptual implementation and might need adjustments based on specific strategy needs.
     *
     * @param <H> Type of the header entity.
     * @param <L> Type of the line entity.
     * @param document The parsed XML document.
     * @param headerEntity The already processed header entity.
     * @param allRules All mapping rules for the interface.
     * @param defaultLineNodeXPath A fallback XPath to find line nodes if derivation fails.
     * @param clientExtractor Function to get the Client from the header entity.
     * @param lineEntityFactory Function to create a new line entity instance.
     * @param lineRuleApplier Consumer to apply mapping rules to a created line entity within the context of its XML element.
     * @return A list of processed line entities.
     * @throws Exception If line processing fails.
     */
    protected <H, L> List<L> processLineItems(
        Document document,
        H headerEntity,
        List<MappingRule> allRules,
        String defaultLineNodeXPath,
        Function<H, Client> clientExtractor,
        BiFunction<H, Client, L> lineEntityFactory,
        BiConsumer<L, Element> lineRuleApplier
    ) throws Exception {

        List<L> lines = new ArrayList<>();
        Client client = clientExtractor.apply(headerEntity);
        if (client == null) {
            throw new ValidationException("Cannot process lines: Client is null in the header entity.");
        }

        List<MappingRule> lineLevelRules = allRules.stream()
            .filter(rule -> rule.getTargetLevel() == TargetLevel.LINE)
            .collect(Collectors.toList());

        if (lineLevelRules.isEmpty()) {
            log.warn("No line-level mapping rules found for document type {}. Skipping line processing.", getDocumentType());
            return lines;
        }

        // Determine line node XPath (Refined logic)
        String lineNodeXPath = determineLineNodeXPath(lineLevelRules, defaultLineNodeXPath);
        log.debug("Using line node XPath: {}", lineNodeXPath);

        NodeList lineNodes = xmlProcessor.evaluateXPathForNodes(document, lineNodeXPath);
        log.debug("Found {} line nodes using XPath: {}", lineNodes.getLength(), lineNodeXPath);

        if (lineNodes.getLength() == 0) {
            log.warn("No line nodes found using XPath: {}. Check mapping rules or XML structure.", lineNodeXPath);
            // Decide if this is an error or acceptable (e.g., header-only document)
            // Depending on requirements, could throw ValidationException here.
        }

        for (int i = 0; i < lineNodes.getLength(); i++) {
            if (!(lineNodes.item(i) instanceof Element)) {
                log.warn("Skipping non-element node found at index {} for XPath {}", i, lineNodeXPath);
                continue;
            }
            Element lineElement = (Element) lineNodes.item(i);

            // Create line entity using the provided factory function
            L lineEntity = lineEntityFactory.apply(headerEntity, client);

            // Apply specific line rules using the provided consumer
            // The consumer should handle iterating through lineLevelRules and calling applyRuleToField
            try {
                 lineRuleApplier.accept(lineEntity, lineElement);
            } catch (Exception e) {
                 log.error("Error applying rules to line item {}: {}", i + 1, e.getMessage(), e);
                 // Decide how to handle line-level errors: skip line, fail document?
                 // For now, rethrow to fail the document processing
                 throw new ValidationException("Failed to apply rules to line item " + (i + 1) + ": " + e.getMessage(), e);
            }

            // Perform any final line validation if needed (e.g., check required fields not covered by rules)
            // validateLineEntity(lineEntity, lineLevelRules);

            lines.add(lineEntity);
        }

        return lines;
    }

    /** Helper to determine the XPath for line nodes. Needs careful implementation. */
    private String determineLineNodeXPath(List<MappingRule> lineRules, String defaultPath) {
        // Simple approach: Use the parent path of the first line rule
        Optional<String> derivedPath = lineRules.stream()
            .map(MappingRule::getSourceField)
            .filter(Objects::nonNull)
            .findFirst()
            .map(path -> {
                int lastSlash = path.lastIndexOf('/');
                // Basic check to avoid returning root or empty path
                return (lastSlash > 0) ? path.substring(0, lastSlash) : defaultPath;
            });

        String path = derivedPath.orElse(defaultPath);
        if (path == null || path.trim().isEmpty()) {
             log.error("Could not determine a valid XPath for line nodes. Default path was also empty/null.");
             throw new ValidationException("Unable to determine XPath for line items. Check mapping rules or provide a default path.");
        }
        return path;
    }

}

