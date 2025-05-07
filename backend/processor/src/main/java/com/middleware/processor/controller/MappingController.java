package com.middleware.processor.controller;

import com.middleware.shared.model.Interface;
import com.middleware.shared.model.MappingRule;
import com.middleware.shared.model.AsnHeader;
import com.middleware.shared.model.AsnLine;
import com.middleware.shared.model.OrderHeader;
import com.middleware.shared.model.OrderLine;
import com.middleware.processor.service.interfaces.XsdService;
import com.middleware.processor.service.interfaces.InterfaceService;
import com.middleware.processor.service.interfaces.MappingRuleService;
import com.middleware.shared.exception.ResourceNotFoundException;
import com.middleware.processor.exception.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.persistence.Column;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMethod;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/mapping")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class MappingController {
    private static final Logger log = LoggerFactory.getLogger(MappingController.class);

    @Autowired
    private XsdService xsdService;

    @Autowired
    private InterfaceService interfaceService;

    @Autowired
    private MappingRuleService mappingRuleService;

    private String getFieldType(Field field) {
        Class<?> type = field.getType();
        if (type == String.class) {
            return "string";
        } else if (type == Integer.class || type == Long.class || type == BigDecimal.class) {
            return "number";
        } else if (type == Boolean.class) {
            return "boolean";
        } else if (type == Date.class) {
            return "date";
        }
        return "string"; // default type
    }

    private List<Map<String, Object>> getFieldsFromEntity(Class<?> entityClass, String tableName) {
        List<Map<String, Object>> fields = new ArrayList<>();
        
        for (Field field : entityClass.getDeclaredFields()) {
            Column column = field.getAnnotation(Column.class);
            if (column != null) {
                String fieldName = column.name().isEmpty() ? field.getName() : column.name();
                Map<String, Object> fieldMap = new HashMap<>();
                fieldMap.put("field", tableName + "." + fieldName);
                fieldMap.put("type", getFieldType(field));
                fieldMap.put("table", tableName);
                fieldMap.put("required", !column.nullable());
                fields.add(fieldMap);
            }
        }
        
        return fields;
    }

    // XSD Structure Endpoints
    @GetMapping("/xsd-structure")
    public ResponseEntity<List<Map<String, Object>>> getXsdStructure(@RequestParam String xsdPath) {
        List<Map<String, Object>> elements = xsdService.getXsdStructure(xsdPath);
        return ResponseEntity.ok(elements);
    }

    @GetMapping("/xsd-structure/{interfaceId}")
    public ResponseEntity<List<Map<String, Object>>> getXsdStructureByInterfaceId(@PathVariable Long interfaceId) {
        Interface interfaceEntity = interfaceService.getInterfaceById(interfaceId)
            .orElseThrow(() -> new ResourceNotFoundException("Interface not found with id: " + interfaceId));
        
        List<Map<String, Object>> elements = xsdService.getXsdStructure(interfaceEntity.getSchemaPath());
        return ResponseEntity.ok(elements);
    }

    @GetMapping("/database-fields")
    public ResponseEntity<List<Map<String, Object>>> getDatabaseFields(
            @RequestParam Long clientId,
            @RequestParam Long interfaceId) {
        
        Interface interfaceEntity = interfaceService.getInterfaceById(interfaceId)
            .orElseThrow(() -> new ResourceNotFoundException("Interface not found with id: " + interfaceId));
        
        List<Map<String, Object>> fields = new ArrayList<>();
        
        // Return different tables based on interface type
        switch (interfaceEntity.getType().toUpperCase()) {
            case "ASN":
                fields.addAll(getFieldsFromEntity(AsnHeader.class, "ASN_HEADERS"));
                fields.addAll(getFieldsFromEntity(AsnLine.class, "ASN_LINES"));
                break;
            case "ORDER":
                fields.addAll(getFieldsFromEntity(OrderHeader.class, "ORDER_HEADERS"));
                fields.addAll(getFieldsFromEntity(OrderLine.class, "ORDER_LINES"));
                break;
            default:
                throw new ValidationException("Unsupported interface type: " + interfaceEntity.getType());
        }
        
        return ResponseEntity.ok(fields);
    }

    // Mapping Rules Endpoints
    @GetMapping("/rules")
    public ResponseEntity<Page<MappingRule>> getMappingRules(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) Long interfaceId,
            @RequestParam(required = false) Boolean isActive) {
        
        log.debug("Getting mapping rules with filters - clientId: {}, interfaceId: {}, isActive: {}", 
                  clientId, interfaceId, isActive);
        
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        
        Page<MappingRule> mappingRules;
        if (clientId != null && interfaceId != null) {
            mappingRules = mappingRuleService.getMappingRulesByClientAndInterface(clientId, interfaceId, pageRequest);
        } else if (clientId != null) {
            mappingRules = mappingRuleService.getMappingRulesByClient(clientId, pageRequest);
        } else if (interfaceId != null) {
            mappingRules = mappingRuleService.getMappingRulesByInterface(interfaceId, pageRequest);
        } else if (isActive != null) {
            mappingRules = mappingRuleService.getMappingRulesByStatus(isActive, pageRequest);
        } else {
            mappingRules = mappingRuleService.getAllMappingRules(pageRequest);
        }
        
        return ResponseEntity.ok(mappingRules);
    }

    @GetMapping("/rules/{id}")
    public ResponseEntity<MappingRule> getMappingRule(@PathVariable Long id) {
        return mappingRuleService.getMappingRuleById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/rules")
    public ResponseEntity<MappingRule> createMappingRule(@RequestBody MappingRule mappingRule) {
        // Backward compatibility: map xmlPath/databaseField if present
        if (mappingRule.getSourceField() == null && mappingRule.getXmlPath() != null) {
            mappingRule.setSourceField(mappingRule.getXmlPath());
        }
        if (mappingRule.getTargetField() == null && mappingRule.getDatabaseField() != null) {
            mappingRule.setTargetField(mappingRule.getDatabaseField());
        }
        MappingRule createdRule = mappingRuleService.createMappingRule(mappingRule);
        return ResponseEntity.ok(createdRule);
    }

    @PutMapping("/rules/{id}")
    public ResponseEntity<MappingRule> updateMappingRule(@PathVariable Long id, @RequestBody MappingRule mappingRule) {
        MappingRule updatedRule = mappingRuleService.updateMappingRule(id, mappingRule);
        return ResponseEntity.ok(updatedRule);
    }

    @DeleteMapping("/rules/{id}")
    public ResponseEntity<Void> deleteMappingRule(@PathVariable Long id) {
        mappingRuleService.deleteMappingRule(id);
        return ResponseEntity.ok().build();
    }
} 
