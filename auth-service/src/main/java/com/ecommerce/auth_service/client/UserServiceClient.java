package com.ecommerce.auth_service.client;

import com.ecommerce.auth_service.client.dto.CreateUserRequest;
import com.ecommerce.auth_service.client.dto.UpdateUserRequest;
import com.ecommerce.auth_service.client.dto.UserAuthResponse;
import com.ecommerce.auth_service.client.dto.UserResponse;
import com.ecommerce.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * Feign Client for User Service
 * Uses Consul for service discovery (name = "user-service")
 */
@FeignClient(name = "user-service", path = "/api/v1/users")
public interface UserServiceClient {

    // ==================== PUBLIC APIs ====================

    @GetMapping("/{userId}")
    ApiResponse<UserResponse> getUserById(@PathVariable("userId") Integer userId);

    @GetMapping("/email/{email}")
    ApiResponse<UserResponse> getUserByEmail(@PathVariable("email") String email);

    @GetMapping("/exists/{email}")
    ApiResponse<Boolean> existsByEmail(@PathVariable("email") String email);

    @PatchMapping("/{userId}")
    ApiResponse<UserResponse> updateUser(
            @PathVariable("userId") Integer userId,
            @RequestBody UpdateUserRequest request);

    // ==================== INTERNAL APIs ====================

    @GetMapping("/internal/auth/email/{email}")
    ApiResponse<UserAuthResponse> getUserAuthByEmail(@PathVariable("email") String email);

    @GetMapping("/internal/auth/{userId}")
    ApiResponse<UserAuthResponse> getUserAuthById(@PathVariable("userId") Integer userId);

    @PostMapping("/internal")
    ApiResponse<UserResponse> createUser(@RequestBody CreateUserRequest request);

    @PutMapping("/internal/{userId}/password")
    ApiResponse<String> updatePassword(
            @PathVariable("userId") Integer userId,
            @RequestBody String newPasswordHash);

    @PutMapping("/internal/{userId}/verify-email")
    ApiResponse<String> verifyEmail(@PathVariable("userId") Integer userId);

    @PutMapping("/internal/{userId}/login")
    ApiResponse<String> updateLoginInfo(
            @PathVariable("userId") Integer userId,
            @RequestParam("ipAddress") String ipAddress);
}
