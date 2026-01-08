package com.ecommerce.common.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Auto-configuration for service-to-service authentication.
 * 
 * This configuration is automatically applied when:
 * - service.auth.enabled=true (default)
 * - Required classes are on the classpath
 * 
 * Beans created:
 * - ServiceAuthProperties - Configuration properties
 * - ServiceAuthClient - Client for obtaining service tokens
 * - FeignServiceAuthInterceptor - Interceptor for Feign clients (if Feign is present)
 * - ServiceAuthFilter - Filter for validating incoming service requests
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(ServiceAuthProperties.class)
@ConditionalOnProperty(prefix = "service.auth", name = "enabled", havingValue = "true", matchIfMissing = false)
public class ServiceAuthAutoConfiguration {

    /**
     * RestTemplate for service auth calls.
     */
    @Bean
    @ConditionalOnMissingBean(name = "serviceAuthRestTemplate")
    public RestTemplate serviceAuthRestTemplate() {
        return new RestTemplate();
    }

    /**
     * Client for obtaining service tokens.
     */
    @Bean
    @ConditionalOnMissingBean(ServiceAuthClient.class)
    public ServiceAuthClient serviceAuthClient(ServiceAuthProperties properties,
                                                RestTemplate serviceAuthRestTemplate) {
        log.info("Configuring ServiceAuthClient for service: {}", properties.getClientId());
        return new ServiceAuthClient(properties, serviceAuthRestTemplate);
    }

    /**
     * Filter for validating incoming service requests.
     */
    @Bean
    @ConditionalOnMissingBean(ServiceAuthFilter.class)
    public ServiceAuthFilter serviceAuthFilter(ServiceAuthProperties properties,
                                                RestTemplate serviceAuthRestTemplate) {
        log.info("Configuring ServiceAuthFilter");
        return new ServiceAuthFilter(properties, serviceAuthRestTemplate);
    }

    /**
     * Feign-specific configuration.
     * Only loaded when Feign is on the classpath.
     */
    @Configuration
    @ConditionalOnClass(name = "feign.RequestInterceptor")
    static class FeignServiceAuthConfiguration {

        @Bean
        @ConditionalOnMissingBean(FeignServiceAuthInterceptor.class)
        public FeignServiceAuthInterceptor feignServiceAuthInterceptor(ServiceAuthClient serviceAuthClient,
                                                                        ServiceAuthProperties properties) {
            log.info("Configuring FeignServiceAuthInterceptor");
            return new FeignServiceAuthInterceptor(serviceAuthClient, properties);
        }
    }
}

