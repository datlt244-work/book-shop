package com.ecommerce.auth_service.controller;

import com.ecommerce.auth_service.dto.request.AuthenticationRequest;
import com.ecommerce.auth_service.dto.request.IntrospectRequest;
import com.ecommerce.auth_service.dto.request.LogoutRequest;
import com.ecommerce.auth_service.dto.request.RefreshTokenRequest;
import com.ecommerce.auth_service.dto.request.RegisterRequest;
import com.ecommerce.auth_service.dto.request.UpdateProfileRequest;
import com.ecommerce.auth_service.dto.response.AuthenticationResponse;
import com.ecommerce.auth_service.dto.response.IntrospectResponse;
import com.ecommerce.auth_service.dto.response.ProfileResponse;
import com.ecommerce.auth_service.dto.response.RegisterResponse;
import com.ecommerce.auth_service.service.AuthenticationService;
import com.ecommerce.common.service.MinioService;
import com.ecommerce.common.dto.ApiResponse;
import com.nimbusds.jose.JOSEException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.text.ParseException;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "APIs for user authentication and authorization")
public class AuthController {
    private final AuthenticationService authenticationService;
    private final MinioService minioService;

    @Operation(summary = "Login", description = "Authenticate user and return JWT token")
    @PostMapping("/login")
    public ApiResponse<AuthenticationResponse> authenticate(
            @Valid @RequestBody AuthenticationRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = getClientIpAddress(httpRequest);
        var result = authenticationService.authenticate(request, clientIp);
        return ApiResponse.<AuthenticationResponse>builder()
                .result(result)
                .build();
    }

    @Operation(summary = "Refresh Token", description = "Get new access token using refresh token")
    @PostMapping("/refresh")
    public ApiResponse<AuthenticationResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        var result = authenticationService.refreshToken(request.getRefreshToken());
        return ApiResponse.<AuthenticationResponse>builder()
                .result(result)
                .build();
    }

    @Operation(summary = "Logout", description = "Invalidate tokens and logout user")
    @PostMapping("/logout")
    public ApiResponse<String> logout(@RequestBody LogoutRequest request) {
        authenticationService.logout(request.getAccessToken(), request.getRefreshToken());
        return ApiResponse.<String>builder()
                .result("Logged out successfully")
                .build();
    }

    @Operation(summary = "Introspect token", description = "Validate and get information about a JWT token")
    @PostMapping("/introspect")
    public ApiResponse<IntrospectResponse> introspect(@Valid @RequestBody IntrospectRequest request)
            throws ParseException, JOSEException {
        var result = authenticationService.introspect(request);
        return ApiResponse.<IntrospectResponse>builder()
                .result(result)
                .build();
    }

    @Operation(summary = "Register new user", description = "Create a new user account")
    @PostMapping("/register")
    public ApiResponse<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        var result = authenticationService.register(request);
        return ApiResponse.<RegisterResponse>builder()
                .result(result)
                .build();
    }

    @Operation(summary = "Get current user profile", description = "Get profile information of the authenticated user", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/me")
    public ApiResponse<ProfileResponse> getProfile(@AuthenticationPrincipal Jwt jwt) {
        Integer userId = extractUserId(jwt);
        var result = authenticationService.getProfile(userId);
        return ApiResponse.<ProfileResponse>builder()
                .result(result)
                .build();
    }

    @Operation(summary = "Update current user profile", description = "Update profile information including optional avatar upload", security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ProfileResponse> updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(value = "fullName", required = false) String fullName,
            @RequestParam(value = "phoneNumber", required = false) String phoneNumber,
            @RequestParam(value = "avatar", required = false) MultipartFile avatar) {

        Integer userId = extractUserId(jwt);

        // Handle avatar upload if provided
        String avatarPath = null;
        if (avatar != null && !avatar.isEmpty()) {
            avatarPath = minioService.uploadAvatar(userId, avatar);
        }

        // Build update request
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName(fullName)
                .phoneNumber(phoneNumber)
                .avatarUrl(avatarPath)
                .build();

        var result = authenticationService.updateProfile(userId, request);
        return ApiResponse.<ProfileResponse>builder()
                .result(result)
                .build();
    }

    private Integer extractUserId(Jwt jwt) {
        Object userIdClaim = jwt.getClaim("userId");
        if (userIdClaim instanceof Long) {
            return ((Long) userIdClaim).intValue();
        } else if (userIdClaim instanceof Integer) {
            return (Integer) userIdClaim;
        } else {
            return Integer.parseInt(userIdClaim.toString());
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        if (request.getRemoteAddr() != null) {
            return request.getRemoteAddr();
        }
        return "unknown";
    }
}
