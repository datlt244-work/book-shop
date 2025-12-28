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
 * Service for managing email verification tokens in Redis
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;

    private static final String VERIFICATION_TOKEN_PREFIX = "auth:email_verification:";
    private static final String RESEND_COOLDOWN_PREFIX = "auth:resend_cooldown:";
    private static final long TOKEN_EXPIRY_HOURS = 24;
    private static final long RESEND_COOLDOWN_MINUTES = 15;

    /**
     * Generate verification token, store in Redis, and send email
     */
    public void sendVerificationEmail(Integer userId, String email, String fullName) {
        // Generate unique token
        String token = UUID.randomUUID().toString();

        // Store token in Redis with user ID as value
        String key = VERIFICATION_TOKEN_PREFIX + token;
        redisTemplate.opsForValue().set(key, userId.toString(), TOKEN_EXPIRY_HOURS, TimeUnit.HOURS);

        // Set cooldown for resend
        String cooldownKey = RESEND_COOLDOWN_PREFIX + email;
        redisTemplate.opsForValue().set(cooldownKey, "1", RESEND_COOLDOWN_MINUTES, TimeUnit.MINUTES);

        log.info("Created email verification token for user {}", userId);

        // Send email asynchronously
        emailService.sendVerificationEmail(email, fullName, token);
    }

    /**
     * Check if user can resend verification email (cooldown check)
     * 
     * @return remaining cooldown in seconds, 0 if can resend
     */
    public long getResendCooldownRemaining(String email) {
        String cooldownKey = RESEND_COOLDOWN_PREFIX + email;
        Long ttl = redisTemplate.getExpire(cooldownKey, TimeUnit.SECONDS);
        return ttl != null && ttl > 0 ? ttl : 0;
    }

    /**
     * Check if can resend and throw exception if in cooldown
     */
    public void checkResendCooldown(String email) {
        long remaining = getResendCooldownRemaining(email);
        if (remaining > 0) {
            long remainingMinutes = (remaining / 60) + 1;
            throw new AppException(ErrorCode.EMAIL_RESEND_COOLDOWN,
                    "Vui lòng đợi " + remainingMinutes + " phút trước khi gửi lại email xác thực");
        }
    }

    /**
     * Validate verification token
     * 
     * @return userId if valid, null otherwise
     */
    public Integer validateToken(String token) {
        String key = VERIFICATION_TOKEN_PREFIX + token;
        String userIdStr = redisTemplate.opsForValue().get(key);

        if (userIdStr == null) {
            log.warn("Verification token not found or expired: {}", token);
            return null;
        }

        return Integer.parseInt(userIdStr);
    }

    /**
     * Invalidate (delete) verification token after successful verification
     */
    public void invalidateToken(String token) {
        String key = VERIFICATION_TOKEN_PREFIX + token;
        redisTemplate.delete(key);
        log.info("Invalidated verification token");
    }

    /**
     * Get remaining time for token (for resend logic)
     */
    public long getTokenRemainingTime(String token) {
        String key = VERIFICATION_TOKEN_PREFIX + token;
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return ttl != null ? ttl : 0;
    }
}
