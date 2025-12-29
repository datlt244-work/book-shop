package com.ecommerce.auth_service.client.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating user via user-service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password hash is required")
    private String passwordHash;

    @Size(max = 100, message = "Full name must be less than 100 characters")
    private String fullName;

    @Size(max = 20, message = "Phone number must be less than 20 characters")
    private String phoneNumber;
}
