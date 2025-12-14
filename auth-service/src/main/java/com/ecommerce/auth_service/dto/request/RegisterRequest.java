package com.ecommerce.auth_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {
    
    @NotBlank(message = "EMAIL_REQUIRED")
    @Email(message = "EMAIL_INVALID")
    @Size(max = 255, message = "EMAIL_TOO_LONG")
    private String email;
    
    @NotBlank(message = "PASSWORD_REQUIRED")
    @Size(min = 8, max = 100, message = "PASSWORD_INVALID")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
        message = "PASSWORD_WEAK"
    )
    private String password;
    
    @NotBlank(message = "FULL_NAME_REQUIRED")
    @Size(min = 1, max = 100, message = "FULL_NAME_INVALID")
    private String fullName;
    
    @Size(max = 20, message = "PHONE_NUMBER_TOO_LONG")
    @Pattern(regexp = "^[0-9+\\-() ]*$", message = "PHONE_NUMBER_INVALID")
    private String phoneNumber;
}
