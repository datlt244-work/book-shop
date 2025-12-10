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
                // Auth Service
                .route("auth-service", r -> r
                        .path("/api/v1/auth/**")
                        .uri("http://localhost:8088"))
                // Product Service
                .route("product-service", r -> r
                        .path("/api/v1/products/**")
                        .uri("http://localhost:8081"))
                // Order Service
                .route("order-service", r -> r
                        .path("/api/v1/orders/**")
                        .uri("http://localhost:8082"))
                // User Service
                .route("user-service", r -> r
                        .path("/api/v1/users/**")
                        .uri("http://localhost:8083"))
                .build();
    }
}

