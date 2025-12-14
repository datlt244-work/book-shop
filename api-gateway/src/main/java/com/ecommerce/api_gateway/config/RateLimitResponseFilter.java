package com.ecommerce.api_gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global filter to handle rate limit exceeded responses.
 * Adds informative headers and a JSON error response when rate limited.
 */
@Slf4j
@Component
public class RateLimitResponseFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            if (exchange.getResponse().getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                String clientIp = exchange.getRequest().getRemoteAddress() != null
                        ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                        : "unknown";
                String path = exchange.getRequest().getPath().value();
                
                log.warn("Rate limit exceeded for IP: {} on path: {}", clientIp, path);
                
                // Add retry-after header (suggest retry in 1 second)
                exchange.getResponse().getHeaders().add("Retry-After", "1");
                exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            }
        }));
    }

    @Override
    public int getOrder() {
        // Run after the rate limiter filter
        return Ordered.LOWEST_PRECEDENCE;
    }
}

