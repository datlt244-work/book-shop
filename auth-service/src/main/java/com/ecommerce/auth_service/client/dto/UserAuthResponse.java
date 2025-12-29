package com.ecommerce.auth_service.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user auth response from user-service (includes passwordHash)
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
