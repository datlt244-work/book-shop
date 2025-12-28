package com.ecommerce.auth_service.service;

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
    private static final long TOKEN_EXPIRY_HOURS = 24;

    /**
     * Generate verification token, store in Redis, and send email
     */
    public void sendVerificationEmail(Integer userId, String email, String fullName) {
        // Generate unique token
        String token = UUID.randomUUID().toString();

        // Store token in Redis with user ID as value
        String key = VERIFICATION_TOKEN_PREFIX + token;
        redisTemplate.opsForValue().set(key, userId.toString(), TOKEN_EXPIRY_HOURS, TimeUnit.HOURS);

        log.info("Created email verification token for user {}", userId);

        // Send email asynchronously
        emailService.sendVerificationEmail(email, fullName, token);
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
