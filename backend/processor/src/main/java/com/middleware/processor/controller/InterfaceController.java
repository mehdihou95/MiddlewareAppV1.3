package com.middleware.processor.controller;

import com.middleware.shared.model.Interface;
import com.middleware.shared.model.MappingRule;
import com.middleware.shared.model.ErrorResponse;
import com.middleware.processor.service.interfaces.InterfaceService;
import com.middleware.processor.validation.InterfaceValidator;
import jakarta.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * REST controller for managing interfaces.
 * Provides endpoints for CRUD operations and interface-specific functionality.
 */
@RestController
@RequestMapping("/api/interfaces")
@Slf4j
public class InterfaceController {

    private final InterfaceService interfaceService;
    private final InterfaceValidator interfaceValidator;

    @Autowired
    public InterfaceController(InterfaceService interfaceService, InterfaceValidator interfaceValidator) {
        this.interfaceService = interfaceService;
        this.interfaceValidator = interfaceValidator;
    }

    @PostMapping
    public ResponseEntity<?> createInterface(@RequestBody Interface interface_) {
        try {
            interfaceValidator.validate(interface_);
            Interface createdInterface = interfaceService.createInterface(interface_);
            return ResponseEntity.ok(createdInterface);
        } catch (ValidationException e) {
            ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                e.getMessage()
            );
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_ERROR",
                "An unexpected error occurred while creating the interface"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getInterface(@PathVariable Long id) {
        try {
            return interfaceService.getInterfaceById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_ERROR",
                "An unexpected error occurred while retrieving the interface"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/client")
    public ResponseEntity<?> getInterfacesByCurrentClient() {
        try {
            // Get the current client ID from the security context
            Long clientId = getCurrentClientId();
            if (clientId == null) {
                return ResponseEntity.badRequest().build();
            }
            
            List<Interface> interfaces = interfaceService.getClientInterfaces(clientId);
            return ResponseEntity.ok(interfaces);
        } catch (Exception e) {
            ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_ERROR",
                "An unexpected error occurred while retrieving interfaces"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<?> getInterfacesByClientId(
            @PathVariable Long clientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestParam(required = false) String searchTerm) {
        
        try {
            PageRequest pageRequest = PageRequest.of(page, size);
            Page<Interface> interfaces = interfaceService.getInterfacesByClient(clientId, pageRequest);
            return ResponseEntity.ok(interfaces);
        } catch (Exception e) {
            ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_ERROR",
                "An unexpected error occurred while retrieving interfaces"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateInterface(@PathVariable Long id, @RequestBody Interface interface_) {
        try {
            interface_.setId(id);
            interfaceValidator.validate(interface_);
            Interface updatedInterface = interfaceService.updateInterface(id, interface_);
            return ResponseEntity.ok(updatedInterface);
        } catch (ValidationException e) {
            ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                e.getMessage()
            );
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_ERROR",
                "An unexpected error occurred while updating the interface"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteInterface(@PathVariable Long id) {
        try {
            interfaceService.deleteInterface(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "NOT_FOUND",
                e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (Exception e) {
            ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_ERROR",
                "An unexpected error occurred while deleting the interface"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/{id}/mappings")
    public ResponseEntity<?> getInterfaceMappings(@PathVariable Long id) {
        try {
            List<MappingRule> mappings = interfaceService.getInterfaceMappings(id);
            return ResponseEntity.ok(mappings);
        } catch (Exception e) {
            ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_ERROR",
                "An unexpected error occurred while retrieving interface mappings"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PutMapping("/{id}/mappings")
    public ResponseEntity<?> updateInterfaceMappings(
        @PathVariable Long id,
        @RequestBody List<MappingRule> mappings
    ) {
        try {
            List<MappingRule> updatedMappings = interfaceService.updateInterfaceMappings(id, mappings);
            return ResponseEntity.ok(updatedMappings);
        } catch (Exception e) {
            ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_ERROR",
                "An unexpected error occurred while updating interface mappings"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    private Long getCurrentClientId() {
        // Get the current client ID from the security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            // Assuming the client ID is stored in the username field
            try {
                return Long.parseLong(userDetails.getUsername());
            } catch (NumberFormatException e) {
                log.error("Failed to parse client ID from username: {}", userDetails.getUsername());
                return null;
            }
        }
        return null;
    }
} 
