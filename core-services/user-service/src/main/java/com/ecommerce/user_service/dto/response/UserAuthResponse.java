package com.ecommerce.user_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for authentication-related user info (internal API)
 * Contains password hash for auth-service to verify
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAuthResponse {
    
    private Integer id;
    private String email;
    private String passwordHash;
    private String role;
    private String status;
    private Boolean emailVerified;
}
