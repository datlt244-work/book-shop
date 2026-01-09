package com.ecommerce.common.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that validates incoming service-to-service authentication.
 * 
 * This filter:
 * - Checks for X-Service-Name header to identify service requests
 * - Validates the service token by calling auth-service introspect endpoint
 * - Sets ServiceAuthentication in SecurityContext if valid
 * 
 * Place this filter BEFORE the standard JWT filter in the filter chain.
 */
@Slf4j
public class ServiceAuthFilter extends OncePerRequestFilter {

    private final ServiceAuthProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public ServiceAuthFilter(ServiceAuthProperties properties) {
        this.properties = properties;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public ServiceAuthFilter(ServiceAuthProperties properties, RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Check if this is a service-to-service request
        String serviceName = request.getHeader(properties.getServiceNameHeader());
        
        if (serviceName == null || serviceName.isEmpty()) {
            // Not a service request, continue with normal filter chain
            filterChain.doFilter(request, response);
            return;
        }

        // This is a service request, validate the token
        String authHeader = request.getHeader(properties.getTokenHeader());
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Service request from {} without valid Authorization header", serviceName);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"code\":401,\"message\":\"Missing or invalid service token\"}");
            return;
        }

        String token = authHeader.substring(7);
        
        try {
            // Validate token with auth-service
            ServiceTokenValidationResult validationResult = validateServiceToken(token);
            
            if (!validationResult.isActive()) {
                log.warn("Invalid service token from claimed service: {}", serviceName);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"code\":401,\"message\":\"Invalid service token\"}");
                return;
            }

            // Verify the claimed service name matches the token
            if (!serviceName.equals(validationResult.getServiceName())) {
                log.warn("Service name mismatch. Header: {}, Token: {}", 
                        serviceName, validationResult.getServiceName());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"code\":401,\"message\":\"Service name mismatch\"}");
                return;
            }

            // Set authentication in security context
            ServiceAuthentication authentication = new ServiceAuthentication(
                    validationResult.getServiceName(),
                    validationResult.getClientId(),
                    null
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            log.debug("Service authenticated: {}", serviceName);

        } catch (Exception e) {
            log.error("Error validating service token", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"code\":500,\"message\":\"Error validating service token\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Validate service token by calling auth-service introspect endpoint.
     */
    private ServiceTokenValidationResult validateServiceToken(String token) {
        try {
            String introspectUrl = properties.getAuthServiceUrl() + "/api/v1/auth/service/introspect";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + token);
            
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    introspectUrl,
                    HttpMethod.POST,
                    request,
                    String.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode responseJson = objectMapper.readTree(response.getBody());
                JsonNode result = responseJson.get("result");
                
                if (result != null) {
                    boolean active = result.get("active").asBoolean();
                    String serviceName = result.has("serviceName") ? result.get("serviceName").asText() : null;
                    String clientId = result.has("clientId") ? result.get("clientId").asText() : null;
                    
                    return new ServiceTokenValidationResult(active, serviceName, clientId);
                }
            }
            
            return new ServiceTokenValidationResult(false, null, null);
            
        } catch (Exception e) {
            log.error("Error calling auth-service introspect", e);
            return new ServiceTokenValidationResult(false, null, null);
        }
    }

    /**
     * Result of token validation.
     */
    private static class ServiceTokenValidationResult {
        private final boolean active;
        private final String serviceName;
        private final String clientId;

        public ServiceTokenValidationResult(boolean active, String serviceName, String clientId) {
            this.active = active;
            this.serviceName = serviceName;
            this.clientId = clientId;
        }

        public boolean isActive() {
            return active;
        }

        public String getServiceName() {
            return serviceName;
        }

        public String getClientId() {
            return clientId;
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Skip filter if service auth is disabled
        return !properties.isEnabled();
    }
}

