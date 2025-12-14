package com.ecommerce.api_gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

/**
 * Configuration for Rate Limiting Key Resolvers.
 * Defines how to identify clients for rate limiting purposes.
 */
@Configuration
public class RateLimiterConfig {

    /**
     * Rate limit by IP address (default).
     * Each unique IP has its own rate limit bucket.
     */
    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            return Mono.just(ip);
        };
    }

    /**
     * Rate limit by User ID from JWT token.
     * Useful for authenticated endpoints where each user has their own limit.
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId == null || userId.isEmpty()) {
                // Fallback to IP if no user ID
                String ip = exchange.getRequest().getRemoteAddress() != null
                        ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                        : "anonymous";
                return Mono.just("anonymous:" + ip);
            }
            return Mono.just("user:" + userId);
        };
    }

    /**
     * Rate limit by API path.
     * Useful for limiting specific endpoints regardless of who calls them.
     */
    @Bean
    public KeyResolver pathKeyResolver() {
        return exchange -> Mono.just(exchange.getRequest().getPath().value());
    }

    /**
     * Combined rate limit by IP + Path.
     * Each IP has separate limits for each path.
     */
    @Bean
    public KeyResolver ipPathKeyResolver() {
        return exchange -> {
            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            String path = exchange.getRequest().getPath().value();
            return Mono.just(ip + ":" + path);
        };
    }
}

