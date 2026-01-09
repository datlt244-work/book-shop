package com.ecommerce.user.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.user.dto.request.CreateProfileRequest;
import com.ecommerce.user.dto.response.UserBasicInfoResponse;
import com.ecommerce.user.dto.response.UserProfileResponse;
import com.ecommerce.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Internal API controller for service-to-service calls.
 * These endpoints require SERVICE role (from ServiceAuthFilter).
 * 
 * Used by:
 * - auth-service: to create profile during registration
 * - order-service: to get user info for orders
 */
@Slf4j
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
@Tag(name = "Internal APIs", description = "Internal APIs for service-to-service communication")
@PreAuthorize("hasRole('SERVICE')")
public class UserInternalController {

    private final UserService userService;

    /**
     * Create a new user profile (called from auth-service during registration)
     */
    @PostMapping("/users")
    @Operation(summary = "Create user profile", description = "Create a new user profile (internal)")
    public ResponseEntity<ApiResponse<UserProfileResponse>> createProfile(
            @Valid @RequestBody CreateProfileRequest request) {
        
        log.info("Internal: Creating profile for user: {}", request.getUserId());
        UserProfileResponse profile = userService.createProfile(request);
        
        return ResponseEntity.ok(ApiResponse.<UserProfileResponse>builder()
                .code(201)
                .message("Profile created successfully")
                .result(profile)
                .build());
    }

    /**
     * Get basic user info (for other services)
     */
    @GetMapping("/users/{userId}/basic")
    @Operation(summary = "Get basic user info", description = "Get basic user info for internal use")
    public ResponseEntity<ApiResponse<UserBasicInfoResponse>> getBasicInfo(
            @PathVariable UUID userId) {
        
        log.debug("Internal: Getting basic info for user: {}", userId);
        UserBasicInfoResponse basicInfo = userService.getBasicInfo(userId);
        
        return ResponseEntity.ok(ApiResponse.<UserBasicInfoResponse>builder()
                .code(200)
                .message("User info retrieved successfully")
                .result(basicInfo)
                .build());
    }

    /**
     * Get full user profile (for admin/internal use)
     */
    @GetMapping("/users/{userId}")
    @Operation(summary = "Get full user profile", description = "Get full user profile for internal use")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
            @PathVariable UUID userId) {
        
        log.debug("Internal: Getting profile for user: {}", userId);
        UserProfileResponse profile = userService.getProfile(userId);
        
        return ResponseEntity.ok(ApiResponse.<UserProfileResponse>builder()
                .code(200)
                .message("Profile retrieved successfully")
                .result(profile)
                .build());
    }

    /**
     * Delete user profile (called when user account is deleted)
     */
    @DeleteMapping("/users/{userId}")
    @Operation(summary = "Delete user profile", description = "Delete user profile (internal)")
    public ResponseEntity<ApiResponse<Void>> deleteProfile(
            @PathVariable UUID userId) {
        
        log.info("Internal: Deleting profile for user: {}", userId);
        userService.deleteProfile(userId);
        
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(200)
                .message("Profile deleted successfully")
                .build());
    }

    /**
     * Check if user exists
     */
    @GetMapping("/users/{userId}/exists")
    @Operation(summary = "Check if user exists", description = "Check if user profile exists")
    public ResponseEntity<ApiResponse<Boolean>> userExists(
            @PathVariable UUID userId) {
        
        try {
            userService.getBasicInfo(userId);
            return ResponseEntity.ok(ApiResponse.<Boolean>builder()
                    .code(200)
                    .message("User exists")
                    .result(true)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.<Boolean>builder()
                    .code(200)
                    .message("User does not exist")
                    .result(false)
                    .build());
        }
    }
}

