package com.ecommerce.auth_service.service;

import com.ecommerce.auth_service.config.VaultConfig;
import com.ecommerce.auth_service.dto.request.AuthenticationRequest;
import com.ecommerce.auth_service.dto.request.IntrospectRequest;
import com.ecommerce.auth_service.dto.request.RegisterRequest;
import com.ecommerce.auth_service.dto.request.UpdateProfileRequest;
import com.ecommerce.auth_service.dto.response.AuthenticationResponse;
import com.ecommerce.auth_service.dto.response.IntrospectResponse;
import com.ecommerce.auth_service.dto.response.ProfileResponse;
import com.ecommerce.auth_service.dto.response.RegisterResponse;
import com.ecommerce.auth_service.entity.User;
import com.ecommerce.auth_service.entity.UserStatus;
import com.ecommerce.auth_service.repository.UserRepository;
import com.ecommerce.common.exception.AppException;
import com.ecommerce.common.exception.ErrorCode;
import com.ecommerce.common.service.MinioService;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final VaultConfig vaultConfig;
    private final TokenRedisService tokenRedisService;
    private final MinioService minioService;
    private final EmailVerificationService emailVerificationService;

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

        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    tokenRedisService.recordFailedLoginAttempt(email);
                    return new AppException(ErrorCode.UNAUTHENTICATED);
                });

        // Check user status with specific error messages
        switch (user.getStatus()) {
            case active -> {
            } // OK, continue
            case pending_verification -> throw new AppException(ErrorCode.EMAIL_NOT_VERIFIED);
            case blocked -> throw new AppException(ErrorCode.ACCOUNT_BLOCKED);
            case inactive -> throw new AppException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }

        // Verify password
        boolean authenticated = passwordEncoder.matches(request.getPassword(), user.getPasswordHash());
        if (!authenticated) {
            int attempts = tokenRedisService.recordFailedLoginAttempt(email);
            int remaining = tokenRedisService.getRemainingAttempts(email);
            log.warn("Failed login attempt {} for user {}. {} attempts remaining", attempts, email, remaining);
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        // Login successful - reset failed attempts
        tokenRedisService.resetLoginAttempts(email);

        // Update login tracking
        user.setLastLoginAt(java.time.LocalDateTime.now());
        user.setLastLoginIp(clientIp);
        user.setLoginCount(user.getLoginCount() + 1);
        userRepository.save(user);

        // Generate tokens
        var accessToken = generateToken(user);
        var refreshToken = tokenRedisService.createRefreshToken(user.getId());

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(vaultConfig.getExpiration())
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .authenticated(true)
                .build();
    }

    // --- 3. REGISTER ---
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail()))
            throw new AppException(ErrorCode.USER_EXISTED);

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .role(com.ecommerce.auth_service.entity.UserRole.customer)
                .status(com.ecommerce.auth_service.entity.UserStatus.pending_verification)
                .emailVerified(false)
                .loginCount(0)
                .build();

        User savedUser = userRepository.save(user);

        return RegisterResponse.builder()
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .fullName(savedUser.getFullName())
                .phoneNumber(savedUser.getPhoneNumber())
                .role(savedUser.getRole().name())
                .status(savedUser.getStatus().name())
                .createdAt(savedUser.getCreatedAt())
                .build();
    }

    // --- 4. REFRESH TOKEN ---
    public AuthenticationResponse refreshToken(String refreshToken) {
        // Validate refresh token
        Integer userId = tokenRedisService.validateRefreshToken(refreshToken);
        if (userId == null) {
            throw new AppException(ErrorCode.TOKEN_INVALID);
        }

        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Check user status
        if (user.getStatus() != com.ecommerce.auth_service.entity.UserStatus.active) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }

        // Invalidate old refresh token
        tokenRedisService.invalidateRefreshToken(refreshToken);

        // Generate new tokens
        var newAccessToken = generateToken(user);
        var newRefreshToken = tokenRedisService.createRefreshToken(user.getId());

        return AuthenticationResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(vaultConfig.getExpiration())
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
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
    public void logoutAllDevices(Integer userId) {
        tokenRedisService.invalidateAllRefreshTokens(userId);
        log.info("User {} logged out from all devices", userId);
    }

    // --- 7. GET PROFILE (UC-10) ---
    public ProfileResponse getProfile(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Generate pre-signed URL for avatar if exists
        String avatarPresignedUrl = minioService.getPresignedUrl(user.getAvatarUrl());

        return ProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(avatarPresignedUrl)
                .role(user.getRole().name())
                .status(user.getStatus().name())
                .emailVerified(user.getEmailVerified())
                .emailVerifiedAt(user.getEmailVerifiedAt())
                .lastLoginAt(user.getLastLoginAt())
                .lastLoginIp(user.getLastLoginIp())
                .loginCount(user.getLoginCount())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    // --- 8. UPDATE PROFILE (UC-11) ---
    public ProfileResponse updateProfile(Integer userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Update only provided fields
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getAvatarUrl() != null) {
            // Store object path in DB (not full URL)
            user.setAvatarUrl(request.getAvatarUrl());
        }

        User savedUser = userRepository.save(user);
        log.info("User {} profile updated", userId);

        // Generate pre-signed URL for avatar
        String avatarPresignedUrl = minioService.getPresignedUrl(savedUser.getAvatarUrl());

        return ProfileResponse.builder()
                .id(savedUser.getId())
                .email(savedUser.getEmail())
                .fullName(savedUser.getFullName())
                .phoneNumber(savedUser.getPhoneNumber())
                .avatarUrl(avatarPresignedUrl)
                .role(savedUser.getRole().name())
                .status(savedUser.getStatus().name())
                .emailVerified(savedUser.getEmailVerified())
                .emailVerifiedAt(savedUser.getEmailVerifiedAt())
                .lastLoginAt(savedUser.getLastLoginAt())
                .lastLoginIp(savedUser.getLastLoginIp())
                .loginCount(savedUser.getLoginCount())
                .createdAt(savedUser.getCreatedAt())
                .updatedAt(savedUser.getUpdatedAt())
                .build();
    }

    // --- Helper: Generate Token ---
    private String generateToken(User user) {
        String jti = UUID.randomUUID().toString();
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getEmail())
                .issuer("com.ecommerce")
                .jwtID(jti)
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(vaultConfig.getExpiration(), ChronoUnit.SECONDS).toEpochMilli()))
                .claim("userId", user.getId())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("scope", user.getRole().name())
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
        Integer userId = emailVerificationService.validateToken(token);
        if (userId == null) {
            throw new AppException(ErrorCode.TOKEN_INVALID);
        }

        // Get user and update verification status
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (user.getEmailVerified()) {
            return "Email already verified";
        }

        user.setEmailVerified(true);
        user.setEmailVerifiedAt(java.time.LocalDateTime.now());

        // Activate account if pending verification
        if (user.getStatus() == UserStatus.pending_verification) {
            user.setStatus(UserStatus.active);
        }

        userRepository.save(user);

        // Invalidate token after successful verification
        emailVerificationService.invalidateToken(token);

        log.info("Email verified for user {}", userId);
        return "Email verified successfully! You can now login.";
    }

    // --- 10. RESEND VERIFICATION EMAIL (UC-06) ---
    public void resendVerificationEmail(String email) {
        // Check rate limiting first
        emailVerificationService.checkResendCooldown(email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (user.getEmailVerified()) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_VERIFIED);
        }

        // Send new verification email
        emailVerificationService.sendVerificationEmail(
                user.getId(),
                user.getEmail(),
                user.getFullName());

        log.info("Resent verification email to {}", email);
    }
}
