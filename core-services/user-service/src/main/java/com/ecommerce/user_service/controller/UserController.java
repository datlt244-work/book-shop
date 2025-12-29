package com.ecommerce.user_service.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.user_service.dto.request.CreateUserRequest;
import com.ecommerce.user_service.dto.request.UpdateUserRequest;
import com.ecommerce.user_service.dto.response.UserAuthResponse;
import com.ecommerce.user_service.dto.response.UserResponse;
import com.ecommerce.user_service.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "APIs for managing users")
public class UserController {
    
    private final UserService userService;
    
    // ==================== PUBLIC APIs ====================
    
    @Operation(summary = "Get user by ID", description = "Get user public information by user ID")
    @GetMapping("/{userId}")
    public ApiResponse<UserResponse> getUserById(@PathVariable Integer userId) {
        var result = userService.getUserById(userId);
        return ApiResponse.<UserResponse>builder()
            .result(result)
            .build();
    }
    
    @Operation(summary = "Get user by email", description = "Get user public information by email")
    @GetMapping("/email/{email}")
    public ApiResponse<UserResponse> getUserByEmail(@PathVariable String email) {
        var result = userService.getUserByEmail(email);
        return ApiResponse.<UserResponse>builder()
            .result(result)
            .build();
    }
    
    @Operation(summary = "Check email exists", description = "Check if email is already registered")
    @GetMapping("/exists/{email}")
    public ApiResponse<Boolean> existsByEmail(@PathVariable String email) {
        var result = userService.existsByEmail(email);
        return ApiResponse.<Boolean>builder()
            .result(result)
            .build();
    }
    
    @Operation(summary = "Update user profile", description = "Update user profile information")
    @PatchMapping("/{userId}")
    public ApiResponse<UserResponse> updateUser(
            @PathVariable Integer userId,
            @Valid @RequestBody UpdateUserRequest request) {
        var result = userService.updateUser(userId, request);
        return ApiResponse.<UserResponse>builder()
            .result(result)
            .build();
    }
    
    // ==================== INTERNAL APIs (for auth-service) ====================
    
    @Operation(summary = "[Internal] Get user auth info", description = "Get user with password hash for authentication - INTERNAL USE ONLY")
    @GetMapping("/internal/auth/email/{email}")
    public ApiResponse<UserAuthResponse> getUserAuthByEmail(@PathVariable String email) {
        var result = userService.getUserAuthByEmail(email);
        return ApiResponse.<UserAuthResponse>builder()
            .result(result)
            .build();
    }
    
    @Operation(summary = "[Internal] Get user auth info by ID", description = "Get user with password hash by ID - INTERNAL USE ONLY")
    @GetMapping("/internal/auth/{userId}")
    public ApiResponse<UserAuthResponse> getUserAuthById(@PathVariable Integer userId) {
        var result = userService.getUserAuthById(userId);
        return ApiResponse.<UserAuthResponse>builder()
            .result(result)
            .build();
    }
    
    @Operation(summary = "[Internal] Create user", description = "Create new user - INTERNAL USE ONLY")
    @PostMapping("/internal")
    public ApiResponse<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        var result = userService.createUser(request);
        return ApiResponse.<UserResponse>builder()
            .result(result)
            .build();
    }
    
    @Operation(summary = "[Internal] Update password", description = "Update user password - INTERNAL USE ONLY")
    @PutMapping("/internal/{userId}/password")
    public ApiResponse<String> updatePassword(
            @PathVariable Integer userId,
            @RequestBody String newPasswordHash) {
        userService.updatePassword(userId, newPasswordHash);
        return ApiResponse.<String>builder()
            .result("Password updated successfully")
            .build();
    }
    
    @Operation(summary = "[Internal] Verify email", description = "Mark user email as verified - INTERNAL USE ONLY")
    @PutMapping("/internal/{userId}/verify-email")
    public ApiResponse<String> verifyEmail(@PathVariable Integer userId) {
        userService.verifyEmail(userId);
        return ApiResponse.<String>builder()
            .result("Email verified successfully")
            .build();
    }
    
    @Operation(summary = "[Internal] Update login info", description = "Update user login tracking - INTERNAL USE ONLY")
    @PutMapping("/internal/{userId}/login")
    public ApiResponse<String> updateLoginInfo(
            @PathVariable Integer userId,
            @RequestParam String ipAddress) {
        userService.updateLoginInfo(userId, ipAddress);
        return ApiResponse.<String>builder()
            .result("Login info updated")
            .build();
    }
}
