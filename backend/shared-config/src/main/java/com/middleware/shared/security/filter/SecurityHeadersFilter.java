package com.middleware.shared.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that adds security headers to HTTP responses.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // Add security headers
        response.setHeader("X-Content-Type-Options", "nosniff");
        
        // Allow H2 Console to be displayed in iframe
        if (request.getRequestURI().startsWith("/h2-console")) {
            response.setHeader("X-Frame-Options", "SAMEORIGIN");
        } else {
            response.setHeader("X-Frame-Options", "DENY");
        }
        
        response.setHeader("X-XSS-Protection", "1; mode=block");
        response.setHeader("Referrer-Policy", "same-origin");
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        
        // Modify CSP for H2 Console
        if (request.getRequestURI().startsWith("/h2-console")) {
            response.setHeader("Content-Security-Policy", 
                "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                "style-src 'self' 'unsafe-inline'; frame-ancestors 'self';");
        } else {
            response.setHeader("Content-Security-Policy", 
                "default-src 'self'; script-src 'self'; object-src 'none'; " +
                "frame-ancestors 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline';");
        }
        
        // Cache control for sensitive pages
        if (request.getRequestURI().startsWith("/api/auth/") || 
            request.getRequestURI().startsWith("/api/user/")) {
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
        }
        
        filterChain.doFilter(request, response);
    }
} 
