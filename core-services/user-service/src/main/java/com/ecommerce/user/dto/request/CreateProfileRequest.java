package com.ecommerce.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

/**
 * Request DTO for creating a user profile (called from auth-service during registration)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProfileRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    private String fullName;

    private String phoneNumber;
}

