package com.ecommerce.api_gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // ==================== API Routes ====================
                
                // Auth Service
                .route("auth-service", r -> r
                        .path("/api/v1/auth/**")
                        .uri("lb://auth-service"))
                
                // Product Service
                .route("product-service", r -> r
                        .path("/api/v1/products/**")
                        .uri("lb://product-service"))
                
                // Order Service
                .route("order-service", r -> r
                        .path("/api/v1/orders/**")
                        .uri("lb://order-service"))
                
                // User Service
                .route("user-service", r -> r
                        .path("/api/v1/users/**")
                        .uri("lb://user-service"))
                
                .build();
    }
}
