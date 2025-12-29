package com.ecommerce.user_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for returning user information
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
