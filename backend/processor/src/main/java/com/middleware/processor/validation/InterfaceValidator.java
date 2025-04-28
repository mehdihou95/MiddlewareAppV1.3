package com.middleware.processor.validation;

import com.middleware.shared.model.Interface;
import com.middleware.shared.repository.InterfaceRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Dedicated validator for Interface entities.
 * Handles both standard Bean Validation and specific business rules for interfaces.
 */
@Component
public class InterfaceValidator {
    
    @Autowired
    private InterfaceRepository interfaceRepository;
    
    @Autowired
    private Validator validator;
    
    /**
     * Validates an interface entity.
     * Performs both standard Bean Validation and specific business rule validation.
     * 
     * @param interface_ The interface to validate
     * @throws ValidationException if validation fails
     */
    public void validate(Interface interface_) {
        // Standard validation with Bean Validation
        Set<ConstraintViolation<Interface>> violations = validator.validate(interface_);
        if (!violations.isEmpty()) {
            throw new ValidationException(violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", ")));
        }
        
        // Specific validation for name uniqueness per client
        if (interface_.getClient() != null && interface_.getClient().getId() != null) {
            // For updates, check if another interface with the same name exists for this client
            if (interface_.getId() != null) {
                boolean exists = interfaceRepository.existsByNameAndClient_IdAndIdNot(
                    interface_.getName(),
                    interface_.getClient().getId(),
                    interface_.getId()
                );
                if (exists) {
                    throw new ValidationException("Another interface with name " + interface_.getName() +
                        " already exists for this client");
                }
            } else {
                // For new interfaces, check if any interface with this name exists for this client
                boolean exists = interfaceRepository.existsByNameAndClient_Id(
                    interface_.getName(),
                    interface_.getClient().getId()
                );
                if (exists) {
                    throw new ValidationException("Interface with name " + interface_.getName() +
                        " already exists for this client");
                }
            }
        }
    }
} 
