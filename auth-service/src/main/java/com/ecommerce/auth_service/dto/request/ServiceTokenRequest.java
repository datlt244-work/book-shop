package com.ecommerce.auth_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Request DTO for service-to-service token generation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceTokenRequest {
    
    @NotBlank(message = "Client ID is required")
    private String clientId;
    
    @NotBlank(message = "Client secret is required")
    private String clientSecret;
}

