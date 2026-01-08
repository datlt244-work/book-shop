package com.ecommerce.common.security;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Feign interceptor that automatically adds service authentication headers
 * to outgoing requests.
 * 
 * This interceptor:
 * - Obtains a service token from ServiceAuthClient
 * - Adds the token as a Bearer token in Authorization header
 * - Adds X-Service-Name header for service identification
 * 
 * Usage:
 * Register this as a bean in your configuration:
 * <pre>
 * @Bean
 * public FeignServiceAuthInterceptor feignServiceAuthInterceptor(ServiceAuthClient client) {
 *     return new FeignServiceAuthInterceptor(client, properties);
 * }
 * </pre>
 */
@Slf4j
@RequiredArgsConstructor
public class FeignServiceAuthInterceptor implements RequestInterceptor {

    private final ServiceAuthClient serviceAuthClient;
    private final ServiceAuthProperties properties;

    @Override
    public void apply(RequestTemplate template) {
        if (!properties.isEnabled()) {
            log.trace("Service auth disabled, skipping interceptor");
            return;
        }

        try {
            String token = serviceAuthClient.getToken();
            
            // Add Authorization header
            template.header(properties.getTokenHeader(), "Bearer " + token);
            
            // Add service name header
            template.header(properties.getServiceNameHeader(), properties.getClientId());
            
            log.trace("Added service auth headers for request to: {}", template.url());
            
        } catch (ServiceAuthClient.ServiceAuthException e) {
            log.error("Failed to add service auth headers: {}", e.getMessage());
            // Depending on requirements, you may want to throw or continue without auth
            throw e;
        }
    }
}

