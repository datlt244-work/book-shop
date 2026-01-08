package com.ecommerce.auth_service.service;

import com.ecommerce.auth_service.client.UserServiceClient;
import com.ecommerce.auth_service.client.dto.CreateUserProfileRequest;
import com.ecommerce.auth_service.client.dto.UserBasicInfo;
import com.ecommerce.auth_service.config.VaultConfig;
import com.ecommerce.auth_service.dto.request.AuthenticationRequest;
import com.ecommerce.auth_service.dto.request.IntrospectRequest;
import com.ecommerce.auth_service.dto.request.RegisterRequest;
import com.ecommerce.auth_service.dto.response.AuthenticationResponse;
import com.ecommerce.auth_service.dto.response.IntrospectResponse;
import com.ecommerce.auth_service.dto.response.RegisterResponse;
import com.ecommerce.auth_service.entity.UserCredential;
import com.ecommerce.auth_service.entity.UserRole;
import com.ecommerce.auth_service.entity.UserStatus;
import com.ecommerce.auth_service.repository.UserCredentialRepository;
import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.common.exception.AppException;
import com.ecommerce.common.exception.ErrorCode;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserCredentialRepository userCredentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final VaultConfig vaultConfig;
    private final TokenRedisService tokenRedisService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;
    private final UserServiceClient userServiceClient;

    // --- 1. INTROSPECT (Verify Token) ---
    public IntrospectResponse introspect(IntrospectRequest request) throws JOSEException, ParseException {
        var token = request.getToken();
        boolean isValid = true;
        try {
            verifyToken(token);
        } catch (AppException | JOSEException | ParseException e) {
            isValid = false;
        }

        return IntrospectResponse.builder()
                .valid(isValid)
                .build();
    }

    // --- 2. AUTHENTICATE (Login) ---
    public AuthenticationResponse authenticate(AuthenticationRequest request, String clientIp) {
        String email = request.getEmail();

        // Check rate limiting
        if (tokenRedisService.isRateLimited(email)) {
            long resetTime = tokenRedisService.getRateLimitResetTime(email);
            log.warn("User {} is rate limited. Reset in {} seconds", email, resetTime);
            throw new AppException(ErrorCode.RATE_LIMITED);
        }

        var userCredential = userCredentialRepository.findByEmail(email)
                .orElseThrow(() -> {
                    tokenRedisService.recordFailedLoginAttempt(email);
                    return new AppException(ErrorCode.UNAUTHENTICATED);
                });

        // Check user status with specific error messages
        switch (userCredential.getStatus()) {
            case active -> {} // OK, continue
            case pending_verification -> throw new AppException(ErrorCode.EMAIL_NOT_VERIFIED);
            case blocked -> throw new AppException(ErrorCode.ACCOUNT_BLOCKED);
            case inactive -> throw new AppException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }

        // Verify password
        boolean authenticated = passwordEncoder.matches(request.getPassword(), userCredential.getPasswordHash());
        if (!authenticated) {
            int attempts = tokenRedisService.recordFailedLoginAttempt(email);
            int remaining = tokenRedisService.getRemainingAttempts(email);
            log.warn("Failed login attempt {} for user {}. {} attempts remaining", attempts, email, remaining);
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        // Login successful - reset failed attempts
        tokenRedisService.resetLoginAttempts(email);

        // Update login tracking
        userCredential.setLastLoginAt(LocalDateTime.now());
        userCredential.setLastLoginIp(clientIp);
        userCredential.setLoginCount(userCredential.getLoginCount() + 1);
        userCredentialRepository.save(userCredential);

        // Fetch user profile from user-service
        String fullName = null;
        String avatarUrl = null;
        try {
            ApiResponse<UserBasicInfo> profileResponse = userServiceClient.getUserBasicInfo(userCredential.getId());
            if (profileResponse != null && profileResponse.getCode() == 200 && profileResponse.getResult() != null) {
                UserBasicInfo profile = profileResponse.getResult();
                fullName = profile.getFullName();
                avatarUrl = profile.getAvatarUrl();
            }
        } catch (Exception e) {
            log.warn("Could not fetch user profile from user-service: {}", e.getMessage());
            // Continue without profile info - not critical for login
        }

        // Generate tokens
        var accessToken = generateToken(userCredential);
        var refreshToken = tokenRedisService.createRefreshToken(userCredential.getId());

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(vaultConfig.getExpiration())
                .userId(userCredential.getId())
                .email(userCredential.getEmail())
                .fullName(fullName)
                .avatarUrl(avatarUrl)
                .role(userCredential.getRole().name())
                .authenticated(true)
                .build();
    }

    // --- 3. REGISTER ---
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userCredentialRepository.existsByEmail(request.getEmail()))
            throw new AppException(ErrorCode.USER_EXISTED);

        // Create user credential
        UserCredential userCredential = UserCredential.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.customer)
                .status(UserStatus.pending_verification)
                .emailVerified(false)
                .loginCount(0)
                .build();

        UserCredential savedCredential = userCredentialRepository.save(userCredential);
        log.info("Created user credential for email: {}", savedCredential.getEmail());

        // Create user profile in user-service
        String fullName = request.getFullName();
        String phoneNumber = request.getPhoneNumber();
        try {
            CreateUserProfileRequest profileRequest = CreateUserProfileRequest.builder()
                    .userId(savedCredential.getId())
                    .email(savedCredential.getEmail())
                    .fullName(fullName)
                    .phoneNumber(phoneNumber)
                    .build();

            ApiResponse<?> profileResponse = userServiceClient.createUserProfile(profileRequest);
            if (profileResponse == null || profileResponse.getCode() != 200) {
                log.error("Failed to create user profile in user-service. Response: {}", profileResponse);
                // Note: We don't rollback credential creation to avoid orphaned profiles
                // A background job can sync later
            } else {
                log.info("Created user profile in user-service for userId: {}", savedCredential.getId());
            }
        } catch (Exception e) {
            log.error("Error creating user profile in user-service: {}", e.getMessage());
            // Continue - profile can be created later via sync
        }

        // Send verification email
        emailVerificationService.sendVerificationEmail(
                savedCredential.getId(),
                savedCredential.getEmail(),
                fullName != null ? fullName : "User");

        return RegisterResponse.builder()
                .userId(savedCredential.getId())
                .email(savedCredential.getEmail())
                .fullName(fullName)
                .phoneNumber(phoneNumber)
                .role(savedCredential.getRole().name())
                .status(savedCredential.getStatus().name())
                .message("Registration successful. Please check your email to verify your account.")
                .createdAt(savedCredential.getCreatedAt())
                .build();
    }

    // --- 4. REFRESH TOKEN ---
    public AuthenticationResponse refreshToken(String refreshToken) {
        // Validate refresh token
        UUID userId = tokenRedisService.validateRefreshToken(refreshToken);
        if (userId == null) {
            throw new AppException(ErrorCode.TOKEN_INVALID);
        }

        // Get user credential
        UserCredential userCredential = userCredentialRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Check user status
        if (userCredential.getStatus() != UserStatus.active) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }

        // Invalidate old refresh token
        tokenRedisService.invalidateRefreshToken(refreshToken);

        // Fetch user profile
        String fullName = null;
        String avatarUrl = null;
        try {
            ApiResponse<UserBasicInfo> profileResponse = userServiceClient.getUserBasicInfo(userId);
            if (profileResponse != null && profileResponse.getCode() == 200 && profileResponse.getResult() != null) {
                fullName = profileResponse.getResult().getFullName();
                avatarUrl = profileResponse.getResult().getAvatarUrl();
            }
        } catch (Exception e) {
            log.warn("Could not fetch user profile during refresh: {}", e.getMessage());
        }

        // Generate new tokens
        var newAccessToken = generateToken(userCredential);
        var newRefreshToken = tokenRedisService.createRefreshToken(userCredential.getId());

        return AuthenticationResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(vaultConfig.getExpiration())
                .userId(userCredential.getId())
                .email(userCredential.getEmail())
                .fullName(fullName)
                .avatarUrl(avatarUrl)
                .role(userCredential.getRole().name())
                .authenticated(true)
                .build();
    }

    // --- 5. LOGOUT ---
    public void logout(String accessToken, String refreshToken) {
        try {
            // Parse token to get JTI and expiration
            SignedJWT signedJWT = SignedJWT.parse(accessToken);
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            String jti = claims.getJWTID();
            if (jti == null) {
                jti = UUID.randomUUID().toString(); // Fallback if no JTI
            }

            Date expiryTime = claims.getExpirationTime();
            long ttlSeconds = (expiryTime.getTime() - System.currentTimeMillis()) / 1000;

            if (ttlSeconds > 0) {
                // Blacklist the access token
                tokenRedisService.blacklistAccessToken(jti, ttlSeconds);
            }
        } catch (ParseException e) {
            log.warn("Could not parse access token for blacklisting: {}", e.getMessage());
        }

        // Invalidate refresh token if provided
        if (refreshToken != null && !refreshToken.isEmpty()) {
            tokenRedisService.invalidateRefreshToken(refreshToken);
        }

        log.info("User logged out successfully");
    }

    // --- 6. LOGOUT ALL DEVICES ---
    public void logoutAllDevices(UUID userId) {
        tokenRedisService.invalidateAllRefreshTokens(userId);
        log.info("User {} logged out from all devices", userId);
    }

    // --- Helper: Generate Token ---
    private String generateToken(UserCredential userCredential) {
        String jti = UUID.randomUUID().toString();
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(userCredential.getEmail())
                .issuer("com.ecommerce")
                .jwtID(jti)
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(vaultConfig.getExpiration(), ChronoUnit.SECONDS).toEpochMilli()))
                .claim("userId", userCredential.getId().toString())
                .claim("email", userCredential.getEmail())
                .claim("role", userCredential.getRole().name())
                .claim("scope", userCredential.getRole().name())
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());

        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            jwsObject.sign(new MACSigner(vaultConfig.getSignerKey().getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Cannot create token", e);
            throw new RuntimeException(e);
        }
    }

    // --- Helper: Verify Token ---
    private void verifyToken(String token) throws JOSEException, ParseException {
        JWSVerifier verifier = new MACVerifier(vaultConfig.getSignerKey().getBytes());

        SignedJWT signedJWT = SignedJWT.parse(token);
        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

        Date expiryTime = claims.getExpirationTime();
        var verified = signedJWT.verify(verifier);

        if (!(verified && expiryTime.after(new Date())))
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        // Check if token is blacklisted
        String jti = claims.getJWTID();
        if (jti != null && tokenRedisService.isAccessTokenBlacklisted(jti)) {
            log.warn("Token {} is blacklisted", jti);
            throw new AppException(ErrorCode.TOKEN_INVALID);
        }
    }

    // --- 9. VERIFY EMAIL (UC-05) ---
    public String verifyEmail(String token) {
        // Validate token and get userId
        UUID userId = emailVerificationService.validateToken(token);
        if (userId == null) {
            throw new AppException(ErrorCode.TOKEN_INVALID);
        }

        // Get user and update verification status
        UserCredential userCredential = userCredentialRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (userCredential.getEmailVerified()) {
            return "Email already verified";
        }

        userCredential.setEmailVerified(true);
        userCredential.setEmailVerifiedAt(LocalDateTime.now());

        // Activate account if pending verification
        if (userCredential.getStatus() == UserStatus.pending_verification) {
            userCredential.setStatus(UserStatus.active);
        }

        userCredentialRepository.save(userCredential);

        // Invalidate token after successful verification
        emailVerificationService.invalidateToken(token);

        log.info("Email verified for user {}", userId);
        return "Email verified successfully! You can now login.";
    }

    // --- 10. RESEND VERIFICATION EMAIL (UC-06) ---
    public void resendVerificationEmail(String email) {
        // Check rate limiting first
        emailVerificationService.checkResendCooldown(email);

        UserCredential userCredential = userCredentialRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (userCredential.getEmailVerified()) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_VERIFIED);
        }

        // Try to get user's name from user-service
        String fullName = "User";
        try {
            ApiResponse<UserBasicInfo> response = userServiceClient.getUserBasicInfo(userCredential.getId());
            if (response != null && response.getCode() == 200 && response.getResult() != null) {
                fullName = response.getResult().getFullName();
            }
        } catch (Exception e) {
            log.warn("Could not fetch user profile for verification email: {}", e.getMessage());
        }

        // Send new verification email
        emailVerificationService.sendVerificationEmail(
                userCredential.getId(),
                userCredential.getEmail(),
                fullName);

        log.info("Resent verification email to {}", email);
    }

    // --- 11. FORGOT PASSWORD (UC-07) ---
    public String forgotPassword(String email) {
        // Always return same message to prevent email enumeration
        String successMessage = "If an account with that email exists, a password reset link has been sent.";

        var userOptional = userCredentialRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            log.info("Password reset requested for non-existent email: {}", email);
            return successMessage; // Don't reveal if email exists
        }

        UserCredential userCredential = userOptional.get();

        // Don't send reset email for blocked accounts
        if (userCredential.getStatus() == UserStatus.blocked) {
            log.warn("Password reset requested for blocked account: {}", email);
            return successMessage;
        }

        // Try to get user's name
        String fullName = "User";
        try {
            ApiResponse<UserBasicInfo> response = userServiceClient.getUserBasicInfo(userCredential.getId());
            if (response != null && response.getCode() == 200 && response.getResult() != null) {
                fullName = response.getResult().getFullName();
            }
        } catch (Exception e) {
            log.warn("Could not fetch user profile for password reset email: {}", e.getMessage());
        }

        // Send password reset email
        passwordResetService.sendPasswordResetEmail(
                userCredential.getId(),
                userCredential.getEmail(),
                fullName);

        log.info("Password reset email sent to {}", email);
        return successMessage;
    }

    // --- 12. RESET PASSWORD (UC-08) ---
    public String resetPassword(String token, String newPassword, String confirmPassword) {
        // Validate passwords match
        if (!newPassword.equals(confirmPassword)) {
            throw new AppException(ErrorCode.PASSWORD_MISMATCH);
        }

        // Validate token and get userId
        UUID userId = passwordResetService.validateToken(token);
        if (userId == null) {
            throw new AppException(ErrorCode.INVALID_RESET_TOKEN);
        }

        // Get user
        UserCredential userCredential = userCredentialRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Update password
        userCredential.setPasswordHash(passwordEncoder.encode(newPassword));
        userCredentialRepository.save(userCredential);

        // Invalidate the token
        passwordResetService.invalidateToken(token);

        // Invalidate all existing tokens for security
        tokenRedisService.invalidateAllUserTokens(userId);

        log.info("Password reset successfully for user {}", userId);
        return "Password reset successfully! You can now login with your new password.";
    }

    // --- 13. CHANGE PASSWORD (UC-09) ---
    public String changePassword(UUID userId, String currentPassword, String newPassword, String confirmPassword) {
        // Validate passwords match
        if (!newPassword.equals(confirmPassword)) {
            throw new AppException(ErrorCode.PASSWORD_MISMATCH);
        }

        // Get user
        UserCredential userCredential = userCredentialRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, userCredential.getPasswordHash())) {
            throw new AppException(ErrorCode.INCORRECT_PASSWORD);
        }

        // Check new password is different from current
        if (passwordEncoder.matches(newPassword, userCredential.getPasswordHash())) {
            throw new AppException(ErrorCode.SAME_PASSWORD);
        }

        // Update password
        userCredential.setPasswordHash(passwordEncoder.encode(newPassword));
        userCredentialRepository.save(userCredential);

        log.info("Password changed successfully for user {}", userId);
        return "Password changed successfully!";
    }
}
