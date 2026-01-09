package com.ecommerce.user.dto.response;

import lombok.*;

import java.util.UUID;

/**
 * Response DTO for basic user info (for service-to-service calls)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBasicInfoResponse {

    private UUID userId;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String avatarUrl;
}

