package com.ecommerce.auth_service.dto.response;

import lombok.*;

/**
 * Response DTO for service token
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceTokenResponse {
    
    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private String serviceName;
}

