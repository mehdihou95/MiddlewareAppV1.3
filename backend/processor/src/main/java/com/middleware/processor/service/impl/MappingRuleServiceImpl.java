package com.middleware.processor.service.impl;

import com.middleware.shared.config.ClientContextHolder;
import com.middleware.shared.model.Interface;
import com.middleware.shared.model.MappingRule;
import com.middleware.shared.repository.InterfaceRepository;
import com.middleware.shared.repository.MappingRuleRepository;
import com.middleware.processor.service.interfaces.MappingRuleService;
import com.middleware.shared.service.util.CircuitBreakerService;
import com.middleware.processor.exception.ValidationException;
import com.middleware.shared.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service("mappingRuleService")
public class MappingRuleServiceImpl implements MappingRuleService {

    private static final Logger log = LoggerFactory.getLogger(MappingRuleServiceImpl.class);

    @Autowired
    private MappingRuleRepository mappingRuleRepository;
    
    @Autowired
    private InterfaceRepository interfaceRepository;
    
    @Autowired
    private CircuitBreakerService circuitBreakerService;

    @Override
    @Transactional
    public MappingRule createMappingRule(MappingRule mappingRule) {
        log.debug("Creating new mapping rule: {}", mappingRule.getName());
        validateMappingRule(mappingRule);
        
        // Set client from context if not provided
        if (mappingRule.getClient() == null && ClientContextHolder.getClient() != null) {
            mappingRule.setClient(ClientContextHolder.getClient());
        }
        
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                try {
                    return mappingRuleRepository.save(mappingRule);
                } catch (Exception e) {
                    log.error("Error creating mapping rule {}: {}", mappingRule.getName(), e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Setting mapping rule {} to inactive", mappingRule.getName());
                mappingRule.setIsActive(false);
                return mappingRule;
            }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MappingRule> getMappingRuleById(Long id) {
        log.debug("Retrieving mapping rule by id: {}", id);
        Long clientId = ClientContextHolder.getClientId();
        
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                try {
                    if (clientId != null) {
                        return mappingRuleRepository.findByIdAndClient_Id(id, clientId);
                    }
                    return mappingRuleRepository.findById(id);
                } catch (Exception e) {
                    log.error("Error retrieving mapping rule by id {}: {}", id, e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Returning empty optional for getMappingRuleById");
                return Optional.empty();
            }
        );
    }

    @Override
    @Transactional
    public MappingRule updateMappingRule(Long id, MappingRule mappingRuleDetails) {
        log.debug("Updating mapping rule with id: {}", id);
        validateMappingRule(mappingRuleDetails);
        
        Long clientId = ClientContextHolder.getClientId();
        
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                try {
                    MappingRule mappingRule;
                    if (clientId != null) {
                        mappingRule = mappingRuleRepository.findByIdAndClient_Id(id, clientId)
                            .orElseThrow(() -> new ResourceNotFoundException("Mapping rule not found with id: " + id));
                    } else {
                        mappingRule = mappingRuleRepository.findById(id)
                            .orElseThrow(() -> new ResourceNotFoundException("Mapping rule not found with id: " + id));
                    }
                    
                    updateMappingRuleFields(mappingRule, mappingRuleDetails);
                    return mappingRuleRepository.save(mappingRule);
                } catch (Exception e) {
                    log.error("Error updating mapping rule with id {}: {}", id, e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Setting mapping rule to inactive");
                mappingRuleDetails.setIsActive(false);
                return mappingRuleDetails;
            }
        );
    }

    @Override
    @Transactional
    public void deleteMappingRule(Long id) {
        log.debug("Deleting mapping rule with id: {}", id);
        Long clientId = ClientContextHolder.getClientId();
        
        circuitBreakerService.executeVoidRepositoryOperation(
            () -> {
                try {
                    if (clientId != null) {
                        mappingRuleRepository.findByIdAndClient_Id(id, clientId)
                            .orElseThrow(() -> new ResourceNotFoundException("Mapping rule not found with id: " + id));
                        mappingRuleRepository.deleteById(id);
                    } else {
                        if (!mappingRuleRepository.existsById(id)) {
                            throw new ResourceNotFoundException("Mapping rule not found with id: " + id);
                        }
                        mappingRuleRepository.deleteById(id);
                    }
                } catch (Exception e) {
                    log.error("Error deleting mapping rule with id {}: {}", id, e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Unable to delete mapping rule with id {}", id);
                throw new ValidationException("Repository service unavailable: Circuit breaker open");
            }
        );
    }
    
    @Override
    @Transactional
    public void saveMappingConfiguration(List<MappingRule> rules) {
        log.debug("Saving mapping configuration with {} rules", rules.size());
        circuitBreakerService.executeRepositoryOperation(
            () -> {
                try {
                    return mappingRuleRepository.saveAll(rules);
                } catch (Exception e) {
                    log.error("Error saving mapping configuration: {}", e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Setting all rules to inactive");
                rules.forEach(rule -> rule.setIsActive(false));
                return rules;
            }
        );
    }
    
    @Override
    @Transactional
    public void deleteByClient_IdAndTableName(Long clientId, String tableName) {
        log.debug("Deleting mapping rules for client {} and table {}", clientId, tableName);
        circuitBreakerService.executeVoidRepositoryOperation(
            () -> {
                try {
                    mappingRuleRepository.deleteByClient_IdAndTableName(clientId, tableName);
                } catch (Exception e) {
                    log.error("Error deleting mapping rules for client {} and table {}: {}", 
                            clientId, tableName, e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Unable to delete mapping rules for client {} and table {}", 
                        clientId, tableName);
                throw new ValidationException("Repository service unavailable: Circuit breaker open");
            }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MappingRule> getAllMappingRules(Pageable pageable) {
        log.debug("Retrieving all mapping rules with pagination");
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                try {
                    return mappingRuleRepository.findAll(pageable);
                } catch (Exception e) {
                    log.error("Error retrieving all mapping rules: {}", e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Returning empty page for getAllMappingRules");
                return Page.empty(pageable);
            }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MappingRule> getMappingRulesByClient(Long clientId, Pageable pageable) {
        log.debug("Retrieving mapping rules for client id: {}", clientId);
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                try {
                    return mappingRuleRepository.findByClient_Id(clientId, pageable);
                } catch (Exception e) {
                    log.error("Error retrieving mapping rules for client {}: {}", clientId, e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Returning empty page for getMappingRulesByClient");
                return Page.empty(pageable);
            }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MappingRule> getMappingRulesByInterface(Long interfaceId, Pageable pageable) {
        log.debug("Retrieving mapping rules for interface id: {}", interfaceId);
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                try {
                    return mappingRuleRepository.findByInterfaceId(interfaceId, pageable);
                } catch (Exception e) {
                    log.error("Error retrieving mapping rules for interface {}: {}", interfaceId, e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Returning empty page for getMappingRulesByInterface");
                return Page.empty(pageable);
            }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MappingRule> getMappingRulesByClientAndInterface(Long clientId, Long interfaceId, Pageable pageable) {
        log.debug("Retrieving mapping rules for client {} and interface {}", clientId, interfaceId);
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
    @Transactional(readOnly = true)
    public Page<MappingRule> searchMappingRules(String name, Pageable pageable) {
        log.debug("Searching mapping rules by name: {}", name);
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                try {
                    return mappingRuleRepository.findByNameContainingIgnoreCase(name, pageable);
                } catch (Exception e) {
                    log.error("Error searching mapping rules by name {}: {}", name, e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Returning empty page for searchMappingRules");
                return Page.empty(pageable);
            }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MappingRule> getMappingRulesByStatus(boolean isActive, Pageable pageable) {
        log.debug("Retrieving mapping rules with status: {}", isActive);
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                try {
                    return mappingRuleRepository.findByIsActive(isActive, pageable);
                } catch (Exception e) {
                    log.error("Error retrieving mapping rules by status {}: {}", isActive, e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Returning empty page for getMappingRulesByStatus");
                return Page.empty(pageable);
            }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MappingRule> getMappingRules(Pageable pageable, String nameFilter, Boolean isActiveFilter) {
        log.debug("Retrieving mapping rules with filters - name: {}, isActive: {}", nameFilter, isActiveFilter);
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                try {
                    if (nameFilter != null && isActiveFilter != null) {
                        return mappingRuleRepository.findByNameContainingIgnoreCaseAndIsActive(nameFilter, isActiveFilter, pageable);
                    } else if (nameFilter != null) {
                        return mappingRuleRepository.findByNameContainingIgnoreCase(nameFilter, pageable);
                    } else if (isActiveFilter != null) {
                        return mappingRuleRepository.findByIsActive(isActiveFilter, pageable);
                    }
                    return mappingRuleRepository.findAll(pageable);
                } catch (Exception e) {
                    log.error("Error retrieving mapping rules with filters: {}", e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Returning empty page for getMappingRules");
                return Page.empty(pageable);
            }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MappingRule> getMappingRulesByClientId(Long clientId, Pageable pageable) {
        log.debug("Retrieving mapping rules for client id: {}", clientId);
        return getMappingRulesByClient(clientId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MappingRule> findByTableNameAndClient_Id(String tableName, Long clientId, Pageable pageable) {
        log.debug("Finding mapping rules by table {} and client {}", tableName, clientId);
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                try {
                    List<MappingRule> rules = mappingRuleRepository.findByTableNameAndClient_Id(tableName, clientId);
                    return createPageFromList(rules, pageable);
                } catch (Exception e) {
                    log.error("Error finding mapping rules by table {} and client {}: {}", 
                            tableName, clientId, e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Returning empty page for findByTableNameAndClient_Id");
                return Page.empty(pageable);
            }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MappingRule> getActiveMappingRules(Long interfaceId, Pageable pageable) {
        log.debug("Retrieving active mapping rules for interface id: {}", interfaceId);
        return circuitBreakerService.<Page<MappingRule>>executeRepositoryOperation(
            () -> {
                try {
                    Interface interfaceEntity = interfaceRepository.findById(interfaceId)
                        .orElseThrow(() -> new ResourceNotFoundException("Interface not found with id: " + interfaceId));
                    
                    // Get active rules for the interface
                    List<MappingRule> rules = mappingRuleRepository.findByInterfaceEntity_IdAndIsActive(interfaceId, true);
                    
                    // Apply pagination
                    int start = (int) pageable.getOffset();
                    int end = Math.min((start + pageable.getPageSize()), rules.size());
                    List<MappingRule> pageContent = rules.subList(start, end);
                    
                    return new PageImpl<>(pageContent, pageable, rules.size());
                } catch (Exception e) {
                    log.error("Error retrieving active mapping rules for interface {}: {}", 
                            interfaceId, e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Returning empty page for getActiveMappingRules");
                return Page.empty(pageable);
            }
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MappingRule> findByClientIdAndInterfaceIdAndTableName(
            Long clientId, 
            Long interfaceId, 
            String tableName, 
            Pageable pageable) {
        log.debug("Finding mapping rules by client {}, interface {}, and table {}", 
            clientId, interfaceId, tableName);
            
        return circuitBreakerService.executeRepositoryOperation(
            () -> {
                try {
                    // First get all rules for the interface
                    List<MappingRule> interfaceRules = mappingRuleRepository.findByInterfaceEntity_Id(interfaceId, Pageable.unpaged())
                        .getContent();
                    
                    // Filter by client and table name
                    List<MappingRule> filteredRules = interfaceRules.stream()
                        .filter(rule -> 
                            rule.getClient().getId().equals(clientId) && 
                            rule.getTableName().equals(tableName))
                        .collect(Collectors.toList());
                    
                    // Apply pagination
                    int start = (int) pageable.getOffset();
                    int end = Math.min((start + pageable.getPageSize()), filteredRules.size());
                    List<MappingRule> pageContent = filteredRules.subList(start, end);
                    
                    return new PageImpl<>(pageContent, pageable, filteredRules.size());
                } catch (Exception e) {
                    log.error("Error finding mapping rules by client {}, interface {}, and table {}: {}", 
                        clientId, interfaceId, tableName, e.getMessage(), e);
                    throw e;
                }
            },
            () -> {
                log.warn("Circuit breaker fallback: Returning empty page for findByClientIdAndInterfaceIdAndTableName");
                return Page.empty(pageable);
            }
        );
    }

    private void validateMappingRule(MappingRule mappingRule) {
        if (mappingRule.getName() == null || mappingRule.getName().trim().isEmpty()) {
            throw new ValidationException("Mapping rule name is required");
        }
        if (mappingRule.getXmlPath() == null || mappingRule.getXmlPath().trim().isEmpty()) {
            throw new ValidationException("XML path is required");
        }
        if (mappingRule.getDatabaseField() == null || mappingRule.getDatabaseField().trim().isEmpty()) {
            throw new ValidationException("Database field is required");
        }
    }

    private void updateMappingRuleFields(MappingRule existingRule, MappingRule newRule) {
        existingRule.setName(newRule.getName());
        existingRule.setDescription(newRule.getDescription());
        existingRule.setXmlPath(newRule.getXmlPath());
        existingRule.setDatabaseField(newRule.getDatabaseField());
        existingRule.setTransformation(newRule.getTransformation());
        existingRule.setRequired(newRule.getRequired());
        existingRule.setDefaultValue(newRule.getDefaultValue());
        existingRule.setPriority(newRule.getPriority());
        existingRule.setSourceField(newRule.getSourceField());
        existingRule.setTargetField(newRule.getTargetField());
        existingRule.setValidationRule(newRule.getValidationRule());
        existingRule.setIsActive(newRule.getIsActive());
        existingRule.setTableName(newRule.getTableName());
        existingRule.setDataType(newRule.getDataType());
        existingRule.setIsAttribute(newRule.getIsAttribute());
        existingRule.setXsdElement(newRule.getXsdElement());
    }

    private Page<MappingRule> createPageFromList(List<MappingRule> rules, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), rules.size());
        List<MappingRule> pageContent = rules.subList(start, end);
        return new PageImpl<>(pageContent, pageable, rules.size());
    }
} 
