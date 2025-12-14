package com.ecommerce.auth_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties loaded from Vault.
 * These values are populated from Vault path: secret/ecommerce/auth-service
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class VaultConfig {
    
    /**
     * JWT signing key loaded from Vault
     */
    private String signerKey;
    
    /**
     * JWT token expiration time in seconds (default: 1 hour)
     */
    private long expiration = 3600;
    
    /**
     * Refresh token expiration time in seconds (default: 24 hours)
     */
    private long refreshExpiration = 86400;
}

