package com.ecommerce.auth_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for service-to-service authentication.
 * Loaded from Vault path: secret/ecommerce/services/auth-service
 */
@Configuration
@ConfigurationProperties(prefix = "service")
@Getter
@Setter
public class ServiceVaultConfig {
    
    /**
     * Service name
     */
    private String name = "auth-service";
    
    /**
     * Client ID for this service
     */
    private String clientId;
    
    /**
     * Client secret for this service
     */
    private String clientSecret;
    
    /**
     * JWT signing key for service tokens
     */
    private String jwtKey;
    
    /**
     * Service token expiration time in seconds (default: 1 hour)
     */
    private long tokenExpiration = 3600;
}

