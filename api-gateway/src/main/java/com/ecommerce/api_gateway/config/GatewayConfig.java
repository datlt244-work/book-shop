package com.ecommerce.api_gateway.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;

@Configuration
public class GatewayConfig {

    private final KeyResolver ipKeyResolver;

    @Value("${gateway.rate-limit.default.replenish-rate:10}")
    private int defaultReplenishRate;

    @Value("${gateway.rate-limit.default.burst-capacity:20}")
    private int defaultBurstCapacity;

    @Value("${gateway.rate-limit.default.requested-tokens:1}")
    private int defaultRequestedTokens;

    @Value("${gateway.rate-limit.auth.replenish-rate:5}")
    private int authReplenishRate;

    @Value("${gateway.rate-limit.auth.burst-capacity:10}")
    private int authBurstCapacity;

    public GatewayConfig(KeyResolver ipKeyResolver) {
        this.ipKeyResolver = ipKeyResolver;
    }

    /**
     * Default rate limiter for general API endpoints.
     * 10 requests per second, burst up to 20.
     */
    @Bean
    @Primary
    @Qualifier("defaultRateLimiter")
    public RedisRateLimiter defaultRateLimiter() {
        return new RedisRateLimiter(defaultReplenishRate, defaultBurstCapacity, defaultRequestedTokens);
    }

    /**
     * Stricter rate limiter for authentication endpoints.
     * 5 requests per second, burst up to 10.
     * Helps prevent brute force attacks.
     */
    @Bean
    @Qualifier("authRateLimiter")
    public RedisRateLimiter authRateLimiter() {
        return new RedisRateLimiter(authReplenishRate, authBurstCapacity, 1);
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder, 
                                           @Qualifier("defaultRateLimiter") RedisRateLimiter defaultRateLimiter,
                                           @Qualifier("authRateLimiter") RedisRateLimiter authRateLimiter) {
        return builder.routes()
                // ==================== Auth Service Routes ====================
                // Login/Register endpoints - stricter rate limiting to prevent brute force
                .route("auth-service-login", r -> r
                        .path("/api/v1/auth/token", "/api/v1/auth/register")
                        .filters(f -> f
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(authRateLimiter)
                                        .setKeyResolver(ipKeyResolver)
                                        .setStatusCode(HttpStatus.TOO_MANY_REQUESTS)
                                        .setDenyEmptyKey(false))
                                .addResponseHeader("X-RateLimit-Type", "auth"))
                        .uri("lb://auth-service"))

                // Other auth endpoints - standard rate limiting
                .route("auth-service", r -> r
                        .path("/api/v1/auth/**")
                        .filters(f -> f
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(defaultRateLimiter)
                                        .setKeyResolver(ipKeyResolver)
                                        .setStatusCode(HttpStatus.TOO_MANY_REQUESTS)
                                        .setDenyEmptyKey(false))
                                .addResponseHeader("X-RateLimit-Type", "default"))
                        .uri("lb://auth-service"))

                // ==================== Product Service Routes ====================
                .route("product-service", r -> r
                        .path("/api/v1/products/**")
                        .filters(f -> f
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(defaultRateLimiter)
                                        .setKeyResolver(ipKeyResolver)
                                        .setStatusCode(HttpStatus.TOO_MANY_REQUESTS)
                                        .setDenyEmptyKey(false))
                                .addResponseHeader("X-RateLimit-Type", "default"))
                        .uri("lb://product-service"))

                // ==================== Order Service Routes ====================
                .route("order-service", r -> r
                        .path("/api/v1/orders/**")
                        .filters(f -> f
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(defaultRateLimiter)
                                        .setKeyResolver(ipKeyResolver)
                                        .setStatusCode(HttpStatus.TOO_MANY_REQUESTS)
                                        .setDenyEmptyKey(false))
                                .addResponseHeader("X-RateLimit-Type", "default"))
                        .uri("lb://order-service"))

                // ==================== User Service Routes ====================
                .route("user-service", r -> r
                        .path("/api/v1/users/**")
                        .filters(f -> f
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(defaultRateLimiter)
                                        .setKeyResolver(ipKeyResolver)
                                        .setStatusCode(HttpStatus.TOO_MANY_REQUESTS)
                                        .setDenyEmptyKey(false))
                                .addResponseHeader("X-RateLimit-Type", "default"))
                        .uri("lb://user-service"))

                .build();
    }
}
