package com.ecommerce.product_service.config;

import com.ecommerce.common.security.FeignServiceAuthInterceptor;
import feign.Logger;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign configuration for service-to-service calls.
 * 
 * The FeignServiceAuthInterceptor is auto-configured by common-lib
 * and automatically adds service authentication headers to all Feign calls.
 */
@Configuration
@EnableFeignClients(basePackages = "com.ecommerce.product_service.client")
public class FeignConfig {

    /**
     * Enable Feign logging for debugging.
     */
    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }
}

