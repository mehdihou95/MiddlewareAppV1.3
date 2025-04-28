package com.middleware.processor.filter;

import com.middleware.shared.config.ClientContextHolder;
import com.middleware.shared.model.Client;
import com.middleware.processor.service.interfaces.ClientService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class ClientContextFilter extends OncePerRequestFilter {

    private static final String CLIENT_ID_HEADER = "X-Client-ID";
    
    private final ClientService clientService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // Extract client ID from header
            String clientIdHeader = request.getHeader(CLIENT_ID_HEADER);
            
            if (clientIdHeader != null && !clientIdHeader.isEmpty()) {
                try {
                    Long clientId = Long.parseLong(clientIdHeader);
                    Optional<Client> clientOpt = clientService.getClientById(clientId);
                    
                    if (clientOpt.isPresent()) {
                        // Set client in context
                        ClientContextHolder.setClient(clientOpt.get());
                        log.debug("Set client context: {}", clientOpt.get().getName());
                    } else {
                        log.warn("Client not found for ID: {}", clientId);
                    }
                } catch (NumberFormatException e) {
                    log.warn("Invalid client ID format: {}", clientIdHeader);
                }
            } else {
                // Clear any existing client context
                ClientContextHolder.clear();
            }
            
            filterChain.doFilter(request, response);
        } finally {
            // Always clear the client context after the request is processed
            ClientContextHolder.clear();
        }
    }
} 
