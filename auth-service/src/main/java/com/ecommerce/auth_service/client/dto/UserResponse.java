package com.ecommerce.auth_service.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for user response from user-service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {

    private Integer id;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String avatarUrl;
    private String role;
    private String status;
    private Boolean emailVerified;
    private LocalDateTime emailVerifiedAt;
    private LocalDateTime lastLoginAt;
    private String lastLoginIp;
    private Integer loginCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
