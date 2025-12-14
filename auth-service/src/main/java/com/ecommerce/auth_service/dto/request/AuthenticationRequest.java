package com.ecommerce.auth_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthenticationRequest {
    
    @NotBlank(message = "USERNAME_REQUIRED")
    @Size(min = 3, max = 50, message = "USERNAME_INVALID")
    private String username;
    
    @NotBlank(message = "PASSWORD_REQUIRED")
    private String password;
}
