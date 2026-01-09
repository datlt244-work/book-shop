package com.ecommerce.user.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for user profile
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private UUID userId;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String avatarUrl;
    private LocalDate dateOfBirth;
    private String bio;
    private List<AddressResponse> addresses;
    private UserPreferencesResponse preferences;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

