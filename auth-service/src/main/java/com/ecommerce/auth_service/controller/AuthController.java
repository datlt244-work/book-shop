package com.ecommerce.auth_service.controller;

import com.ecommerce.auth_service.dto.request.AuthenticationRequest;
import com.ecommerce.auth_service.dto.request.IntrospectRequest;
import com.ecommerce.auth_service.dto.request.LogoutRequest;
import com.ecommerce.auth_service.dto.request.RefreshTokenRequest;
import com.ecommerce.auth_service.dto.request.RegisterRequest;
import com.ecommerce.auth_service.dto.request.ResendVerificationRequest;
import com.ecommerce.auth_service.dto.request.ForgotPasswordRequest;
import com.ecommerce.auth_service.dto.request.ResetPasswordRequest;
import com.ecommerce.auth_service.dto.request.ChangePasswordRequest;
import com.ecommerce.auth_service.dto.response.AuthenticationResponse;
import com.ecommerce.auth_service.dto.response.IntrospectResponse;
import com.ecommerce.auth_service.dto.response.RegisterResponse;
import com.ecommerce.auth_service.service.AuthenticationService;
import com.ecommerce.common.dto.ApiResponse;
import com.nimbusds.jose.JOSEException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.UUID;

/**
 * Authentication Controller
 * 
 * Note: Profile management (get/update profile, avatar upload) has been moved to user-service.
 * This controller handles authentication-only concerns.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "APIs for user authentication and authorization")
public class AuthController {
    
    private final AuthenticationService authenticationService;

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

    @Operation(summary = "Register new user", description = "Create a new user account and send verification email")
    @PostMapping("/register")
    public ApiResponse<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        var result = authenticationService.register(request);
        return ApiResponse.<RegisterResponse>builder()
                .result(result)
                .build();
    }

    @Operation(summary = "Verify email", description = "Verify user email address using token from email")
    @GetMapping("/verify-email")
    public ApiResponse<String> verifyEmail(@RequestParam(name = "token") String token) {
        var result = authenticationService.verifyEmail(token);
        return ApiResponse.<String>builder()
                .result(result)
                .build();
    }

    @Operation(summary = "Resend verification email", description = "Resend email verification link to unverified account")
    @PostMapping("/resend-verification")
    public ApiResponse<String> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        authenticationService.resendVerificationEmail(request.getEmail());
        return ApiResponse.<String>builder()
                .result("Verification email sent successfully. Please check your inbox.")
                .build();
    }

    @Operation(summary = "Forgot password", description = "Request password reset email")
    @PostMapping("/forgot-password")
    public ApiResponse<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        var result = authenticationService.forgotPassword(request.getEmail());
        return ApiResponse.<String>builder()
                .result(result)
                .build();
    }

    @Operation(summary = "Reset password", description = "Reset password using token from email")
    @PostMapping("/reset-password")
    public ApiResponse<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        var result = authenticationService.resetPassword(
                request.getToken(),
                request.getNewPassword(),
                request.getConfirmPassword());
        return ApiResponse.<String>builder()
                .result(result)
                .build();
    }

    @Operation(summary = "Change password", description = "Change password for authenticated user", 
               security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/change-password")
    public ApiResponse<String> changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangePasswordRequest request) {
        UUID userId = extractUserId(jwt);
        var result = authenticationService.changePassword(
                userId,
                request.getCurrentPassword(),
                request.getNewPassword(),
                request.getConfirmPassword());
        return ApiResponse.<String>builder()
                .result(result)
                .build();
    }

    @Operation(summary = "Logout from all devices", description = "Invalidate all tokens for the user",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/logout-all")
    public ApiResponse<String> logoutAllDevices(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = extractUserId(jwt);
        authenticationService.logoutAllDevices(userId);
        return ApiResponse.<String>builder()
                .result("Logged out from all devices successfully")
                .build();
    }

    private UUID extractUserId(Jwt jwt) {
        Object userIdClaim = jwt.getClaim("userId");
        if (userIdClaim instanceof String) {
            return UUID.fromString((String) userIdClaim);
        }
        // Fallback for legacy tokens with numeric userId
        return UUID.fromString(userIdClaim.toString());
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
