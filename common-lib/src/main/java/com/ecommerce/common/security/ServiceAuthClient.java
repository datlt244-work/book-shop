package com.ecommerce.common.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Client for obtaining and caching service-to-service authentication tokens.
 * 
 * This client handles:
 * - Requesting tokens from auth-service
 * - Caching tokens to avoid repeated requests
 * - Automatically refreshing tokens before expiration
 * 
 * Usage:
 * <pre>
 * String token = serviceAuthClient.getToken();
 * headers.set("Authorization", "Bearer " + token);
 * </pre>
 */
@Slf4j
public class ServiceAuthClient {

    private final ServiceAuthProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    // Cached token
    private volatile ServiceTokenInfo cachedToken;
    private final ReentrantLock tokenLock = new ReentrantLock();

    public ServiceAuthClient(ServiceAuthProperties properties) {
        this.properties = properties;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public ServiceAuthClient(ServiceAuthProperties properties, RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Get a valid service token, refreshing if necessary.
     * 
     * @return JWT token string
     * @throws ServiceAuthException if unable to obtain token
     */
    public String getToken() {
        if (!properties.isEnabled()) {
            throw new ServiceAuthException("Service authentication is disabled");
        }
        
        // Check if we have a valid cached token
        if (cachedToken != null && !cachedToken.shouldRefresh(properties.getRefreshThresholdSeconds())) {
            return cachedToken.getToken();
        }
        
        // Need to refresh token
        return refreshToken();
    }

    /**
     * Force refresh the token.
     */
    public String refreshToken() {
        tokenLock.lock();
        try {
            // Double-check after acquiring lock
            if (cachedToken != null && !cachedToken.shouldRefresh(properties.getRefreshThresholdSeconds())) {
                return cachedToken.getToken();
            }
            
            log.debug("Requesting new service token for: {}", properties.getClientId());
            
            String tokenUrl = properties.getAuthServiceUrl() + "/api/v1/auth/service/token";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, String> body = new HashMap<>();
            body.put("clientId", properties.getClientId());
            body.put("clientSecret", properties.getClientSecret());
            
            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    request,
                    String.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode responseJson = objectMapper.readTree(response.getBody());
                JsonNode result = responseJson.get("result");
                
                if (result != null) {
                    String token = result.get("accessToken").asText();
                    long expiresIn = result.get("expiresIn").asLong();
                    String serviceName = result.get("serviceName").asText();
                    
                    cachedToken = ServiceTokenInfo.builder()
                            .token(token)
                            .serviceName(serviceName)
                            .clientId(properties.getClientId())
                            .expiresAt(Instant.now().plusSeconds(expiresIn))
                            .build();
                    
                    log.info("Obtained new service token for: {}, expires in: {} seconds", 
                            serviceName, expiresIn);
                    
                    return token;
                }
            }
            
            throw new ServiceAuthException("Failed to obtain service token: invalid response");
            
        } catch (ServiceAuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error obtaining service token", e);
            throw new ServiceAuthException("Failed to obtain service token: " + e.getMessage(), e);
        } finally {
            tokenLock.unlock();
        }
    }

    /**
     * Get the current cached token info (may be null or expired).
     */
    public ServiceTokenInfo getCachedTokenInfo() {
        return cachedToken;
    }

    /**
     * Clear the cached token.
     */
    public void clearCache() {
        tokenLock.lock();
        try {
            cachedToken = null;
            log.debug("Service token cache cleared");
        } finally {
            tokenLock.unlock();
        }
    }

    /**
     * Add service authentication headers to an HttpHeaders object.
     */
    public void addAuthHeaders(HttpHeaders headers) {
        String token = getToken();
        headers.set(properties.getTokenHeader(), "Bearer " + token);
        headers.set(properties.getServiceNameHeader(), properties.getClientId());
    }

    /**
     * Exception for service authentication failures.
     */
    public static class ServiceAuthException extends RuntimeException {
        public ServiceAuthException(String message) {
            super(message);
        }
        
        public ServiceAuthException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

