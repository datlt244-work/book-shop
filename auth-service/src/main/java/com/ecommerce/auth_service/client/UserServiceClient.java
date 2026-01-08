package com.ecommerce.auth_service.client;

import com.ecommerce.auth_service.client.dto.CreateUserProfileRequest;
import com.ecommerce.auth_service.client.dto.UserBasicInfo;
import com.ecommerce.auth_service.client.dto.UserProfileInfo;
import com.ecommerce.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Feign client for user-service internal APIs.
 * 
 * Uses service discovery (Consul) to find user-service.
 * Service authentication is handled by FeignServiceAuthInterceptor.
 */
@FeignClient(
        name = "user-service",
        path = "/api/v1/internal",
        fallbackFactory = UserServiceClientFallback.class
)
public interface UserServiceClient {

    /**
     * Create a new user profile (called during registration)
     */
    @PostMapping("/users")
    ApiResponse<UserProfileInfo> createUserProfile(@RequestBody CreateUserProfileRequest request);

    /**
     * Get basic user info (for login response)
     */
    @GetMapping("/users/{userId}/basic")
    ApiResponse<UserBasicInfo> getUserBasicInfo(@PathVariable("userId") UUID userId);

    /**
     * Get full user profile
     */
    @GetMapping("/users/{userId}")
    ApiResponse<UserProfileInfo> getUserProfile(@PathVariable("userId") UUID userId);

    /**
     * Delete user profile (when account is deleted)
     */
    @DeleteMapping("/users/{userId}")
    ApiResponse<Void> deleteUserProfile(@PathVariable("userId") UUID userId);

    /**
     * Check if user exists
     */
    @GetMapping("/users/{userId}/exists")
    ApiResponse<Boolean> userExists(@PathVariable("userId") UUID userId);
}

