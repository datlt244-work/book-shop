package com.ecommerce.auth_service.client.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for full user profile from user-service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileInfo {
    
    private UUID userId;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String avatarUrl;
    private LocalDate dateOfBirth;
    private String bio;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

