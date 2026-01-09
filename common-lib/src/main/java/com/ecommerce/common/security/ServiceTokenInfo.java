package com.ecommerce.common.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Holds information about a service token.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceTokenInfo {
    
    private String token;
    private String serviceName;
    private String clientId;
    private Instant expiresAt;
    
    /**
     * Check if the token is expired or about to expire.
     * 
     * @param thresholdSeconds consider expired if expires within this many seconds
     * @return true if token should be refreshed
     */
    public boolean shouldRefresh(long thresholdSeconds) {
        if (token == null || expiresAt == null) {
            return true;
        }
        return Instant.now().plusSeconds(thresholdSeconds).isAfter(expiresAt);
    }
    
    /**
     * Check if token is completely expired.
     */
    public boolean isExpired() {
        return expiresAt == null || Instant.now().isAfter(expiresAt);
    }
}

