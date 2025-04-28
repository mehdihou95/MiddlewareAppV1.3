package com.middleware.listener.connectors.sftp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class SftpRouteService {
    private final CamelContext camelContext;

    /**
     * Safely shuts down all routes associated with a specific client and interface combination.
     * This includes the main route, directory-specific routes, and connection test routes.
     *
     * @param clientId The ID of the client
     * @param interfaceId The ID of the interface
     */
    public void shutdownRoutes(Long clientId, Long interfaceId) {
        String routeBaseId = String.format("sftp-%d-%d", clientId, interfaceId);
        
        camelContext.getRoutes().stream()
            .filter(route -> route.getId().startsWith(routeBaseId))
            .forEach(route -> {
                try {
                    // First stop the route with a timeout
                    camelContext.getRouteController().stopRoute(route.getId(), 10, TimeUnit.SECONDS, true);
                    log.info("Successfully stopped route: {}", route.getId());
                    
                    // Then remove it from context
                    camelContext.removeRoute(route.getId());
                    log.info("Successfully removed route: {}", route.getId());
                } catch (Exception e) {
                    log.error("Error shutting down route {}: {}", route.getId(), e.getMessage());
                    throw new RuntimeException("Failed to shutdown SFTP route: " + route.getId(), e);
                }
            });
    }
} 