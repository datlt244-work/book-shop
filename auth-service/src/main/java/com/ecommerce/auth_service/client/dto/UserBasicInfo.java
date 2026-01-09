package com.ecommerce.auth_service.client.dto;

import lombok.*;

import java.util.UUID;

/**
 * DTO for basic user info from user-service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBasicInfo {
    
    private UUID userId;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String avatarUrl;
}

