package com.ecommerce.auth_service.service;

import com.ecommerce.common.exception.AppException;
import com.ecommerce.common.exception.ErrorCode;
import com.ecommerce.common.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing password reset tokens in Redis
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;

    private static final String RESET_TOKEN_PREFIX = "auth:password_reset:";
    private static final String RESET_COOLDOWN_PREFIX = "auth:reset_cooldown:";
    private static final long TOKEN_EXPIRY_HOURS = 1; // Token expires in 1 hour
    private static final long COOLDOWN_MINUTES = 5; // Cooldown between requests

    /**
     * Generate password reset token, store in Redis, and send email
     */
    public void sendPasswordResetEmail(Integer userId, String email, String fullName) {
        // Check cooldown
        checkResetCooldown(email);

        // Generate unique token
        String token = UUID.randomUUID().toString();

        // Store token in Redis with user ID as value
        String key = RESET_TOKEN_PREFIX + token;
        redisTemplate.opsForValue().set(key, userId.toString(), TOKEN_EXPIRY_HOURS, TimeUnit.HOURS);

        // Set cooldown for resend
        String cooldownKey = RESET_COOLDOWN_PREFIX + email;
        redisTemplate.opsForValue().set(cooldownKey, "1", COOLDOWN_MINUTES, TimeUnit.MINUTES);

        log.info("Created password reset token for user {}", userId);

        // Send email asynchronously
        emailService.sendPasswordResetEmail(email, fullName, token);
    }

    /**
     * Check if user can request password reset (cooldown check)
     */
    private void checkResetCooldown(String email) {
        String cooldownKey = RESET_COOLDOWN_PREFIX + email;
        Long ttl = redisTemplate.getExpire(cooldownKey, TimeUnit.SECONDS);
        if (ttl != null && ttl > 0) {
            long remainingMinutes = (ttl / 60) + 1;
            throw new AppException(ErrorCode.PASSWORD_RESET_COOLDOWN,
                    "Vui lòng đợi " + remainingMinutes + " phút trước khi yêu cầu lại");
        }
    }

    /**
     * Validate password reset token
     * 
     * @return userId if valid, null otherwise
     */
    public Integer validateToken(String token) {
        String key = RESET_TOKEN_PREFIX + token;
        String userIdStr = redisTemplate.opsForValue().get(key);

        if (userIdStr == null) {
            log.warn("Password reset token not found or expired: {}", token);
            return null;
        }

        return Integer.parseInt(userIdStr);
    }

    /**
     * Invalidate (delete) password reset token after successful reset
     */
    public void invalidateToken(String token) {
        String key = RESET_TOKEN_PREFIX + token;
        redisTemplate.delete(key);
        log.info("Invalidated password reset token");
    }

    /**
     * Invalidate all tokens for a user (when password is reset)
     * Note: This is a simplified implementation. For production,
     * consider storing user-token mapping for complete invalidation.
     */
    public void invalidateAllUserTokens(Integer userId) {
        // In a more complete implementation, we would track all tokens per user
        // For now, the token will naturally expire
        log.info("Password reset completed for user {}", userId);
    }
}
