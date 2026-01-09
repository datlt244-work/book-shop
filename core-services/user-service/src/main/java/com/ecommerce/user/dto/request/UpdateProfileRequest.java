package com.ecommerce.user.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

/**
 * Request DTO for updating user profile
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @Size(max = 100, message = "Full name cannot exceed 100 characters")
    private String fullName;

    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    private String phoneNumber;

    @Size(max = 500, message = "Avatar URL cannot exceed 500 characters")
    private String avatarUrl;

    private LocalDate dateOfBirth;

    @Size(max = 500, message = "Bio cannot exceed 500 characters")
    private String bio;
}

