package com.ecommerce.common.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for service-to-service authentication.
 * 
 * Configure in application.yaml:
 * <pre>
 * service:
 *   auth:
 *     enabled: true
 *     client-id: my-service
 *     client-secret: secret-from-vault
 *     auth-service-url: http://auth-service:8088
 * </pre>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "service.auth")
public class ServiceAuthProperties {
    
    /**
     * Enable/disable service authentication
     */
    private boolean enabled = true;
    
    /**
     * Client ID for this service
     */
    private String clientId;
    
    /**
     * Client secret for this service
     */
    private String clientSecret;
    
    /**
     * URL of auth service to get tokens
     */
    private String authServiceUrl = "http://auth-service:8088";
    
    /**
     * Token refresh threshold in seconds.
     * Refresh token when remaining lifetime is less than this.
     */
    private long refreshThresholdSeconds = 300; // 5 minutes
    
    /**
     * Header name for service token
     */
    private String tokenHeader = "Authorization";
    
    /**
     * Header name for service name identification
     */
    private String serviceNameHeader = "X-Service-Name";
}

