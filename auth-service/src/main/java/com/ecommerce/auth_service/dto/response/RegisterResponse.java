package com.ecommerce.auth_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterResponse {
    private Integer userId;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String role;
    private String status;
    private LocalDateTime createdAt;
}
