package com.middleware.shared.security.service;

import com.middleware.shared.config.ClientContextHolder;
import com.middleware.shared.exception.TokenException;
import com.middleware.shared.security.model.TokenType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.security.Key;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service responsible for JWT token generation, validation, and parsing.
 * <p>
 * This service handles all JWT token operations including:
 * <ul>
 *   <li>Generating access tokens</li>
 *   <li>Generating refresh tokens</li>
 *   <li>Validating tokens</li>
 *   <li>Extracting claims from tokens</li>
 * </ul>
 * <p>
 * Token expiration times are configured via application properties.
 */
@Slf4j
@Service
public class JwtService {

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;
    
    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;
    
    @Value("${application.security.jwt.refresh-token.expiration}")
    private long refreshExpiration;

    /**
     * Extracts the username from a JWT token.
     *
     * @param token the JWT token
     * @return the username
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts a specific claim from a JWT token.
     *
     * @param token the JWT token
     * @param claimsResolver function to extract the desired claim
     * @return the extracted claim
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Generates an access token for the specified user.
     *
     * @param userDetails the user details
     * @return a JWT access token
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(userDetails, TokenType.ACCESS);
    }

    /**
     * Generates a token of the specified type for the user.
     *
     * @param userDetails the user details
     * @param tokenType the type of token to generate
     * @return a JWT token
     */
    public String generateToken(UserDetails userDetails, TokenType tokenType) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", tokenType.name());
        
        // Add roles to token, stripping ROLE_ prefix
        claims.put("roles", userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .map(role -> role.replace("ROLE_", ""))
            .collect(Collectors.toList()));
        
        // Add client context if available
        Long clientId = ClientContextHolder.getClientId();
        if (clientId != null) {
            claims.put("clientId", clientId);
        }
        
        // Add fingerprint for access tokens
        if (tokenType == TokenType.ACCESS) {
            claims.put("fingerprint", generateFingerprint(userDetails));
        }
        
        long expiration = switch(tokenType) {
            case ACCESS -> jwtExpiration;
            case REFRESH -> refreshExpiration;
            case RESET_PASSWORD -> 900000; // 15 minutes
            case EMAIL_VERIFICATION -> 86400000; // 24 hours
            case REMEMBER_ME -> 2592000000L; // 30 days
        };
        
        return buildToken(claims, userDetails, expiration);
    }

    /**
     * Generates a refresh token for the specified user.
     *
     * @param userDetails the user details
     * @return a JWT refresh token
     */
    public String generateRefreshToken(UserDetails userDetails) {
        return generateToken(userDetails, TokenType.REFRESH);
    }

    /**
     * Builds a JWT token with the specified claims, user details, and expiration.
     *
     * @param extraClaims additional claims to include in the token
     * @param userDetails the user details
     * @param expiration the token expiration time in milliseconds
     * @return a JWT token
     */
    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration
    ) {
        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey())
                .compact();
    }

    /**
     * Validates a JWT token for the specified user.
     *
     * @param token the JWT token
     * @param userDetails the user details
     * @return true if the token is valid, false otherwise
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final Claims claims = extractAllClaims(token);
            final String username = claims.getSubject();
            final Date expiration = claims.getExpiration();
            
            log.info("Validating token for user: {} with roles: {}", username, userDetails.getAuthorities());
            
            // First check basic validity: matching username and not expired
            if (!username.equals(userDetails.getUsername())) {
                log.warn("Token username ({}) doesn't match userDetails username ({})", username, userDetails.getUsername());
                return false;
            }
            
            if (expiration.before(new Date())) {
                log.warn("Token expired at: {}", expiration);
                return false;
            }
            
            // For access tokens, validate fingerprint if present
            // This helps prevent token theft by binding the token to the original request context
            if (claims.containsKey("type") && "ACCESS".equals(claims.get("type"))) {
                log.info("Validating ACCESS token");
                if (claims.containsKey("fingerprint")) {
                    // Generate expected fingerprint based on current request
                    String expectedFingerprint = generateFingerprint(userDetails);
                    String tokenFingerprint = claims.get("fingerprint", String.class);
                    
                    log.info("Fingerprint validation - Token: {}, Expected: {}", 
                        tokenFingerprint, expectedFingerprint);
                    
                    // If fingerprints don't match, token might be stolen
                    if (expectedFingerprint == null || !expectedFingerprint.equals(tokenFingerprint)) {
                        log.warn("Token fingerprint mismatch for user: {}", username);
                        return false;
                    }
                } else {
                    log.info("No fingerprint in ACCESS token");
                }
            }
            
            log.info("Token validated successfully for user: {}", username);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("Token expired: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Error validating token: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Extracts the expiration date from a JWT token.
     *
     * @param token the JWT token
     * @return the expiration date
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Checks if a JWT token is expired.
     *
     * @param token the JWT token
     * @return true if the token is expired, false otherwise
     */
    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    /**
     * Extracts all claims from a JWT token.
     *
     * @param token the JWT token
     * @return the claims
     * @throws TokenException if the token is invalid
     */
    public Claims extractAllClaims(String token) {
        try {
            return Jwts
                    .parserBuilder()
                    .setSigningKey(getSignInKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw TokenException.expired();
        } catch (UnsupportedJwtException | MalformedJwtException | io.jsonwebtoken.security.SignatureException | IllegalArgumentException e) {
            throw TokenException.invalid();
        }
    }

    /**
     * Gets the signing key for JWT token generation and validation.
     *
     * @return the signing key
     */
    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Gets the token from the current authentication.
     *
     * @param authentication the current authentication
     * @return the token
     */
    public String getTokenFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return (String) authentication.getCredentials();
    }

    /**
     * Extracts the token from the HTTP request.
     *
     * @param request the HTTP request
     * @return the token
     */
    public String extractTokenFromRequest(HttpServletRequest request) {
        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7);
    }

    /**
     * Generates a fingerprint for the current request context.
     *
     * @param userDetails the user details
     * @return the fingerprint
     */
    private String generateFingerprint(UserDetails userDetails) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            log.warn("No RequestAttributes found while generating fingerprint");
            return null;
        }

        HttpServletRequest request = attributes.getRequest();
        String userAgent = request.getHeader("User-Agent");
        String ipAddress = request.getRemoteAddr();
        String username = userDetails.getUsername();

        String fingerprintData = String.format("%s:%s:%s", username, userAgent, ipAddress);
        String fingerprint = DigestUtils.sha256Hex(fingerprintData);
        
        log.info("Generated fingerprint for user {}: {} (from UA: {}, IP: {})", 
            username, fingerprint, userAgent, ipAddress);
        
        return fingerprint;
    }

    /**
     * Generates a test token for development purposes.
     *
     * @return a test token
     */
    public String generateTestToken() {
        return generateToken(new UserDetails() {
            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return Collections.singletonList(new SimpleGrantedAuthority("ROLE_TEST"));
            }

            @Override
            public String getPassword() {
                return null;
            }

            @Override
            public String getUsername() {
                return "test";
            }

            @Override
            public boolean isAccountNonExpired() {
                return true;
            }

            @Override
            public boolean isAccountNonLocked() {
                return true;
            }

            @Override
            public boolean isCredentialsNonExpired() {
                return true;
            }

            @Override
            public boolean isEnabled() {
                return true;
            }
        });
    }

    /**
     * Validates a test token.
     *
     * @param token the token to validate
     * @return true if the token is a valid test token
     */
    public boolean validateTestToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return "test".equals(claims.getSubject()) &&
                   claims.get("roles", List.class).contains("ROLE_TEST");
        } catch (Exception e) {
            return false;
        }
    }
} 
