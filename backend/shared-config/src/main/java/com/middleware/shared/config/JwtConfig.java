package com.middleware.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;

    @Value("${application.security.jwt.refresh-token.expiration}")
    private long refreshTokenExpiration;

    public String getSecretKey() {
        return secretKey;
    }

    public long getJwtExpiration() {
        return jwtExpiration;
    }

    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }
} 
