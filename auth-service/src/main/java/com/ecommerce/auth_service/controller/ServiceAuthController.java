package com.ecommerce.auth_service.controller;

import com.ecommerce.auth_service.dto.request.ServiceTokenRequest;
import com.ecommerce.auth_service.dto.response.ServiceTokenResponse;
import com.ecommerce.auth_service.service.ServiceTokenService;
import com.ecommerce.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for service-to-service authentication.
 * 
 * This controller provides endpoints for microservices to authenticate
 * with each other using client credentials.
 */
@Slf4j
@RestController
@RequestMapping("/auth/service")
@RequiredArgsConstructor
@Tag(name = "Service Authentication", description = "APIs for service-to-service authentication")
public class ServiceAuthController {

    private final ServiceTokenService serviceTokenService;

    /**
     * Generate a service token using client credentials.
     * 
     * Services call this endpoint with their clientId and clientSecret
     * to obtain a JWT token for authenticating with other services.
     */
    @PostMapping("/token")
    @Operation(
        summary = "Get service token",
        description = "Exchange service credentials for a JWT token"
    )
    public ResponseEntity<ApiResponse<ServiceTokenResponse>> getServiceToken(
            @Valid @RequestBody ServiceTokenRequest request) {
        
        log.debug("Service token request from client: {}", request.getClientId());
        
        ServiceTokenResponse response = serviceTokenService.generateServiceToken(request);
        
        return ResponseEntity.ok(ApiResponse.<ServiceTokenResponse>builder()
                .code(200)
                .message("Service token generated successfully")
                .result(response)
                .build());
    }

    /**
     * Introspect a service token to validate it.
     */
    @PostMapping("/introspect")
    @Operation(
        summary = "Validate service token",
        description = "Check if a service token is valid"
    )
    public ResponseEntity<ApiResponse<ServiceTokenIntrospectResponse>> introspectServiceToken(
            @RequestHeader("Authorization") String authHeader) {
        
        // Extract token from Bearer header
        String token = authHeader;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        
        ServiceTokenService.ServiceTokenInfo tokenInfo = serviceTokenService.validateToken(token);
        
        if (tokenInfo == null) {
            return ResponseEntity.ok(ApiResponse.<ServiceTokenIntrospectResponse>builder()
                    .code(200)
                    .message("Token validation result")
                    .result(ServiceTokenIntrospectResponse.builder()
                            .active(false)
                            .build())
                    .build());
        }
        
        return ResponseEntity.ok(ApiResponse.<ServiceTokenIntrospectResponse>builder()
                .code(200)
                .message("Token validation result")
                .result(ServiceTokenIntrospectResponse.builder()
                        .active(true)
                        .serviceName(tokenInfo.serviceName())
                        .clientId(tokenInfo.clientId())
                        .build())
                .build());
    }

    /**
     * Response DTO for token introspection
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ServiceTokenIntrospectResponse {
        private boolean active;
        private String serviceName;
        private String clientId;
    }
}

