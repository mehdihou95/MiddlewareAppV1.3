package com.middleware.shared.security.filter;

import com.middleware.shared.security.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import io.jsonwebtoken.Claims;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT Authentication Filter that processes JWT tokens from the Authorization header
 * and sets up the security context based on the authentication.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        log.info("Processing request: {} {}", request.getMethod(), request.getRequestURI());
        
        try {
            // Try both case versions of Authorization header
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null) {
                authHeader = request.getHeader("authorization");
            }
            
            log.info("Authorization header: {}", authHeader);
            
            // If no auth header or not a bearer token, continue the chain
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.info("No valid Authorization header found. Chain continuing as anonymous.");
                filterChain.doFilter(request, response);
                return;
            }

            // Extract the JWT token
            String jwt = authHeader.substring(7);
            log.info("JWT token found: {}", jwt.substring(0, Math.min(10, jwt.length())) + "...");
            
            // Extract the username from the token
            String username = jwtService.extractUsername(jwt);
            log.info("Username from token: {}", username);
            
            // Only process if username was extracted and no authentication exists
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Load the user details
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                log.info("Loaded user details: {} with authorities: {}", userDetails.getUsername(), userDetails.getAuthorities());
                
                // Validate the token
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    // Get roles from token
                    Claims claims = jwtService.extractAllClaims(jwt);
                    @SuppressWarnings("unchecked")
                    List<String> roles = (List<String>) claims.get("roles", List.class);
                    List<SimpleGrantedAuthority> authorities = roles.stream()
                         .map(role -> "ROLE_" + role)  // Add ROLE_ prefix since roles from token don't have it
                         .map(SimpleGrantedAuthority::new)
                         .collect(Collectors.toList());
                    
                    log.info("Using roles from token: {}", roles);
                    
                    // Create authentication token with roles from the token
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            jwt,  // Store the JWT token as credentials
                            authorities
                    );
                    
                    // Add request details
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // Set the authentication in the security context
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.info("Authentication set in SecurityContext: {}", authToken);
                    log.info("User roles: {}", authorities);
                } else {
                    log.warn("JWT token validation failed for user: {}", username);
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
                    return;
                }
            }
        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token expired");
            return;
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token format");
            return;
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unsupported token");
            return;
        } catch (Exception e) {
            log.error("Error processing JWT token: {}", e.getMessage(), e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            return;
        }
        
        log.info("Security context before continuing filter chain: {}", 
                SecurityContextHolder.getContext().getAuthentication());
        
        // Continue the filter chain
        filterChain.doFilter(request, response);
    }
} 
