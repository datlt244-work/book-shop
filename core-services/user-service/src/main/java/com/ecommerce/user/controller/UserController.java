package com.ecommerce.user.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.user.dto.request.CreateAddressRequest;
import com.ecommerce.user.dto.request.UpdateAddressRequest;
import com.ecommerce.user.dto.request.UpdateProfileRequest;
import com.ecommerce.user.dto.response.AddressResponse;
import com.ecommerce.user.dto.response.UserBasicInfoResponse;
import com.ecommerce.user.dto.response.UserProfileResponse;
import com.ecommerce.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Public API controller for user profile management.
 * All endpoints require user JWT authentication.
 */
@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "User profile management APIs")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    // ==================== PROFILE ENDPOINTS ====================

    /**
     * Get current user's profile
     */
    @GetMapping("/me")
    @Operation(summary = "Get my profile", description = "Get current authenticated user's profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(
            @AuthenticationPrincipal Jwt jwt) {
        
        UUID userId = extractUserId(jwt);
        UserProfileResponse profile = userService.getProfile(userId);
        
        return ResponseEntity.ok(ApiResponse.<UserProfileResponse>builder()
                .code(200)
                .message("Profile retrieved successfully")
                .result(profile)
                .build());
    }

    /**
     * Update current user's profile
     */
    @PutMapping("/me")
    @Operation(summary = "Update my profile", description = "Update current authenticated user's profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMyProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateProfileRequest request) {
        
        UUID userId = extractUserId(jwt);
        UserProfileResponse profile = userService.updateProfile(userId, request);
        
        return ResponseEntity.ok(ApiResponse.<UserProfileResponse>builder()
                .code(200)
                .message("Profile updated successfully")
                .result(profile)
                .build());
    }

    /**
     * Get public user profile by ID (limited info)
     */
    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID", description = "Get public user profile by ID")
    public ResponseEntity<ApiResponse<UserBasicInfoResponse>> getUserById(
            @PathVariable UUID userId) {
        
        UserBasicInfoResponse basicInfo = userService.getBasicInfo(userId);
        
        return ResponseEntity.ok(ApiResponse.<UserBasicInfoResponse>builder()
                .code(200)
                .message("User info retrieved successfully")
                .result(basicInfo)
                .build());
    }

    // ==================== ADDRESS ENDPOINTS ====================

    /**
     * Get all addresses for current user
     */
    @GetMapping("/me/addresses")
    @Operation(summary = "Get my addresses", description = "Get all addresses for current user")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getMyAddresses(
            @AuthenticationPrincipal Jwt jwt) {
        
        UUID userId = extractUserId(jwt);
        List<AddressResponse> addresses = userService.getAddresses(userId);
        
        return ResponseEntity.ok(ApiResponse.<List<AddressResponse>>builder()
                .code(200)
                .message("Addresses retrieved successfully")
                .result(addresses)
                .build());
    }

    /**
     * Get a specific address
     */
    @GetMapping("/me/addresses/{addressId}")
    @Operation(summary = "Get address by ID", description = "Get a specific address")
    public ResponseEntity<ApiResponse<AddressResponse>> getAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID addressId) {
        
        UUID userId = extractUserId(jwt);
        AddressResponse address = userService.getAddress(userId, addressId);
        
        return ResponseEntity.ok(ApiResponse.<AddressResponse>builder()
                .code(200)
                .message("Address retrieved successfully")
                .result(address)
                .build());
    }

    /**
     * Create a new address
     */
    @PostMapping("/me/addresses")
    @Operation(summary = "Create address", description = "Create a new address")
    public ResponseEntity<ApiResponse<AddressResponse>> createAddress(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateAddressRequest request) {
        
        UUID userId = extractUserId(jwt);
        AddressResponse address = userService.createAddress(userId, request);
        
        return ResponseEntity.ok(ApiResponse.<AddressResponse>builder()
                .code(201)
                .message("Address created successfully")
                .result(address)
                .build());
    }

    /**
     * Update an address
     */
    @PutMapping("/me/addresses/{addressId}")
    @Operation(summary = "Update address", description = "Update an existing address")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID addressId,
            @Valid @RequestBody UpdateAddressRequest request) {
        
        UUID userId = extractUserId(jwt);
        AddressResponse address = userService.updateAddress(userId, addressId, request);
        
        return ResponseEntity.ok(ApiResponse.<AddressResponse>builder()
                .code(200)
                .message("Address updated successfully")
                .result(address)
                .build());
    }

    /**
     * Delete an address
     */
    @DeleteMapping("/me/addresses/{addressId}")
    @Operation(summary = "Delete address", description = "Delete an address")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID addressId) {
        
        UUID userId = extractUserId(jwt);
        userService.deleteAddress(userId, addressId);
        
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(200)
                .message("Address deleted successfully")
                .build());
    }

    // ==================== HELPER METHODS ====================

    private UUID extractUserId(Jwt jwt) {
        Object userIdClaim = jwt.getClaim("userId");
        if (userIdClaim instanceof Integer) {
            // Legacy: convert Integer to UUID (for backward compatibility)
            // In new system, userId should be UUID
            throw new IllegalStateException("Legacy integer userId not supported. Please use UUID.");
        } else if (userIdClaim instanceof String) {
            return UUID.fromString((String) userIdClaim);
        } else if (userIdClaim instanceof UUID) {
            return (UUID) userIdClaim;
        }
        throw new IllegalStateException("Cannot extract userId from JWT");
    }
}

