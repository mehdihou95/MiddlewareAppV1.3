package com.middleware.processor.controller;

import com.middleware.shared.security.service.JwtService;
import com.middleware.shared.model.RefreshTokenRequest;
import com.middleware.shared.security.service.JwtBlacklistService;
import com.middleware.processor.service.impl.SecurityLoggerServiceImpl;
import com.middleware.shared.security.RateLimiter;
import com.middleware.shared.security.service.CsrfTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final JwtBlacklistService jwtBlacklistService;
    private final SecurityLoggerServiceImpl securityLogger;
    private final RateLimiter rateLimiter;
    private final CsrfTokenService csrfTokenService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request, 
                                                   HttpServletRequest servletRequest, 
                                                   HttpServletResponse response) {
        String ipAddress = servletRequest.getRemoteAddr();
        String rateLimitKey = ipAddress + ":" + request.getUsername();

        if (!rateLimiter.checkRateLimit(rateLimitKey)) {
            securityLogger.logSuspiciousActivity(request.getUsername(), "Rate limit exceeded", ipAddress);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of("error", "Too many login attempts. Please try again later."));
        }

        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            
            String jwt = jwtService.generateToken(userDetails);
            String refreshToken = jwtService.generateRefreshToken(userDetails);

            // Generate new CSRF token
            CsrfToken csrfToken = csrfTokenService.generateToken(servletRequest, response);
            
            response.setHeader(csrfToken.getHeaderName(), csrfToken.getToken());
            
            securityLogger.logSuccessfulLogin(userDetails.getUsername(), ipAddress);
            rateLimiter.resetLimit(rateLimitKey);

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("token", jwt);
            responseBody.put("refreshToken", refreshToken);
            responseBody.put("username", userDetails.getUsername());
            responseBody.put("roles", userDetails.getAuthorities().stream()
                .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                .collect(Collectors.toList()));
            if (csrfToken != null) {
                responseBody.put("csrfToken", csrfToken.getToken());
            }
            
            return ResponseEntity.ok(responseBody);
        } catch (AuthenticationException e) {
            securityLogger.logFailedLogin(request.getUsername(), ipAddress);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid username or password"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshToken(
            @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        try {
            String username = jwtService.extractUsername(request.getRefreshToken());
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            
            if (jwtService.isTokenValid(request.getRefreshToken(), userDetails)) {
                String newToken = jwtService.generateToken(userDetails);
                String newRefreshToken = jwtService.generateRefreshToken(userDetails);
                
                securityLogger.logTokenRefresh(username, true, httpRequest.getRemoteAddr());
                
                Map<String, Object> response = new HashMap<>();
                response.put("token", newToken);
                response.put("refreshToken", newRefreshToken);
                response.put("username", userDetails.getUsername());
                response.put("roles", userDetails.getAuthorities().stream()
                    .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                    .collect(Collectors.toList()));
                
                return ResponseEntity.ok(response);
            }
            
            securityLogger.logTokenRefresh(username, false, httpRequest.getRemoteAddr());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid refresh token"));
        } catch (Exception e) {
            securityLogger.logSuspiciousActivity("unknown", "Invalid refresh token format", 
                httpRequest.getRemoteAddr());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Error processing refresh token"));
        }
    }
    
    // For form-based login compatibility
    @PostMapping(value = "/login", consumes = "application/x-www-form-urlencoded")
    public ResponseEntity<Map<String, Object>> formLogin(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {
        LoginRequest loginRequest = new LoginRequest(username, password);
        return login(loginRequest, httpRequest, response);
    }

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            Map<String, Object> response = new HashMap<>();
            
            // Get roles from authentication
            List<String> roles = userDetails.getAuthorities().stream()
                .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                .collect(Collectors.toList());
            
            response.put("valid", true);
            response.put("username", userDetails.getUsername());
            response.put("roles", roles);
            
            // Log roles for debugging
            logger.info("User {} validated with roles: {}", userDetails.getUsername(), roles);
            
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(Map.of("valid", false, "error", "Invalid or expired token"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest httpRequest, HttpServletResponse response) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                String username = authentication.getName();
                
                // Get the token from the Authorization header
                String authHeader = httpRequest.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String jwt = authHeader.substring(7);
                    jwtBlacklistService.blacklistToken(jwt);
                }
                
                securityLogger.logLogout(username, httpRequest.getRemoteAddr());
                csrfTokenService.clearToken(httpRequest, response);
                
                // Clear the security context
                SecurityContextHolder.clearContext();
                
                return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
            }
            return ResponseEntity.ok(Map.of("message", "No active session to logout"));
        } catch (Exception e) {
            logger.error("Error during logout", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error during logout"));
        }
    }

    @PostMapping("/refresh-csrf")
    public ResponseEntity<?> refreshCsrfToken(HttpServletRequest request, HttpServletResponse response) {
        CsrfToken newToken = csrfTokenService.generateToken(request, response);
        
        // Return token in both header and body
        return ResponseEntity.ok()
                .header(newToken.getHeaderName(), newToken.getToken())
                .body(Map.of("csrfToken", newToken.getToken()));
    }
}

class LoginRequest {
    private String username;
    private String password;
    
    public LoginRequest() {}
    
    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
}

