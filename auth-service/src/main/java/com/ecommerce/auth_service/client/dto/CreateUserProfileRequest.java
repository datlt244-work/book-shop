package com.ecommerce.auth_service.client.dto;

import lombok.*;

import java.util.UUID;

/**
 * Request DTO for creating user profile in user-service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserProfileRequest {
    
    private UUID userId;
    private String email;
    private String fullName;
    private String phoneNumber;
}

