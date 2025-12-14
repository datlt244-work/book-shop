package com.ecommerce.auth_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {
    
    @NotBlank(message = "USERNAME_REQUIRED")
    @Size(min = 3, max = 50, message = "USERNAME_INVALID")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "USERNAME_FORMAT_INVALID")
    private String username;
    
    @NotBlank(message = "PASSWORD_REQUIRED")
    @Size(min = 8, max = 100, message = "PASSWORD_INVALID")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
        message = "PASSWORD_WEAK"
    )
    private String password;
    
    @NotBlank(message = "FIRST_NAME_REQUIRED")
    @Size(min = 1, max = 100, message = "FIRST_NAME_INVALID")
    private String firstName;
    
    @NotBlank(message = "LAST_NAME_REQUIRED")
    @Size(min = 1, max = 100, message = "LAST_NAME_INVALID")
    private String lastName;
    
    @NotNull(message = "DOB_REQUIRED")
    @Past(message = "DOB_INVALID")
    private LocalDate dob;
}
