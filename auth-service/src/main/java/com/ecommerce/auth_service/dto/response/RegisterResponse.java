package com.ecommerce.auth_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterResponse {
    private UUID userId;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String role;
    private String status;
    private String message;
    private LocalDateTime createdAt;
}
