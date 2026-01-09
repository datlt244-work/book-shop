package com.ecommerce.auth_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Note: userId is now UUID (stored as String in Redis)
 */

/**
 * Service for managing tokens using Redis
 * - Access Token Blacklist (for logout)
 * - Refresh Token Storage
 * - Login Rate Limiting
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenRedisService {

    private final StringRedisTemplate stringRedisTemplate;

    // Redis key prefixes
    private static final String BLACKLIST_PREFIX = "auth:blacklist:";
    private static final String REFRESH_TOKEN_PREFIX = "auth:refresh:";
    private static final String USER_REFRESH_PREFIX = "auth:user_refresh:";
    private static final String LOGIN_ATTEMPTS_PREFIX = "auth:login_attempts:";
    private static final String USER_SESSION_PREFIX = "auth:session:";

    // Default TTL values
    private static final long REFRESH_TOKEN_TTL_DAYS = 7;
    private static final long BLACKLIST_TTL_HOURS = 24;
    private static final long LOGIN_ATTEMPT_WINDOW_MINUTES = 15;
    private static final int MAX_LOGIN_ATTEMPTS = 5;

    // ==================== ACCESS TOKEN BLACKLIST ====================

    /**
     * Add an access token to blacklist (used when user logs out)
     * Token will be automatically removed after TTL
     */
    public void blacklistAccessToken(String jti, long ttlSeconds) {
        String key = BLACKLIST_PREFIX + jti;
        stringRedisTemplate.opsForValue().set(key, "blacklisted", ttlSeconds, TimeUnit.SECONDS);
        log.info("Token {} added to blacklist, TTL: {} seconds", jti, ttlSeconds);
    }

    /**
     * Check if an access token is blacklisted
     */
    public boolean isAccessTokenBlacklisted(String jti) {
        String key = BLACKLIST_PREFIX + jti;
        Boolean exists = stringRedisTemplate.hasKey(key);
        return exists != null && exists;
    }

    // ==================== REFRESH TOKEN MANAGEMENT ====================

    /**
     * Store a refresh token for a user
     * Returns the generated refresh token
     */
    public String createRefreshToken(UUID userId) {
        String refreshToken = UUID.randomUUID().toString();
        String tokenKey = REFRESH_TOKEN_PREFIX + refreshToken;
        String userKey = USER_REFRESH_PREFIX + userId.toString();

        // Store refresh token -> userId mapping
        stringRedisTemplate.opsForValue().set(
                tokenKey,
                userId.toString(),
                Duration.ofDays(REFRESH_TOKEN_TTL_DAYS));

        // Store userId -> refresh token mapping (for invalidation)
        stringRedisTemplate.opsForSet().add(userKey, refreshToken);
        stringRedisTemplate.expire(userKey, Duration.ofDays(REFRESH_TOKEN_TTL_DAYS));

        log.info("Created refresh token for user {}", userId);
        return refreshToken;
    }

    /**
     * Validate and get userId from refresh token
     * Returns null if token is invalid or expired
     */
    public UUID validateRefreshToken(String refreshToken) {
        String key = REFRESH_TOKEN_PREFIX + refreshToken;
        String userId = stringRedisTemplate.opsForValue().get(key);

        if (userId == null) {
            log.warn("Invalid or expired refresh token");
            return null;
        }

        return UUID.fromString(userId);
    }

    /**
     * Invalidate a specific refresh token
     */
    public void invalidateRefreshToken(String refreshToken) {
        String tokenKey = REFRESH_TOKEN_PREFIX + refreshToken;
        String userId = stringRedisTemplate.opsForValue().get(tokenKey);

        if (userId != null) {
            // Remove from user's refresh token set
            String userKey = USER_REFRESH_PREFIX + userId;
            stringRedisTemplate.opsForSet().remove(userKey, refreshToken);
        }

        // Delete the token
        stringRedisTemplate.delete(tokenKey);
        log.info("Invalidated refresh token");
    }

    /**
     * Invalidate all refresh tokens for a user (logout from all devices)
     */
    public void invalidateAllRefreshTokens(UUID userId) {
        String userKey = USER_REFRESH_PREFIX + userId.toString();
        var tokens = stringRedisTemplate.opsForSet().members(userKey);

        if (tokens != null) {
            for (String token : tokens) {
                stringRedisTemplate.delete(REFRESH_TOKEN_PREFIX + token);
            }
        }

        stringRedisTemplate.delete(userKey);
        log.info("Invalidated all refresh tokens for user {}", userId);
    }

    // ==================== LOGIN RATE LIMITING ====================

    /**
     * Record a failed login attempt
     * Returns the current number of attempts
     */
    public int recordFailedLoginAttempt(String email) {
        String key = LOGIN_ATTEMPTS_PREFIX + email;
        Long attempts = stringRedisTemplate.opsForValue().increment(key);

        if (attempts != null && attempts == 1) {
            // First attempt, set expiration
            stringRedisTemplate.expire(key, Duration.ofMinutes(LOGIN_ATTEMPT_WINDOW_MINUTES));
        }

        return attempts != null ? attempts.intValue() : 1;
    }

    /**
     * Check if user is rate limited
     */
    public boolean isRateLimited(String email) {
        String key = LOGIN_ATTEMPTS_PREFIX + email;
        String attempts = stringRedisTemplate.opsForValue().get(key);

        if (attempts == null) {
            return false;
        }

        return Integer.parseInt(attempts) >= MAX_LOGIN_ATTEMPTS;
    }

    /**
     * Get remaining attempts for a user
     */
    public int getRemainingAttempts(String email) {
        String key = LOGIN_ATTEMPTS_PREFIX + email;
        String attempts = stringRedisTemplate.opsForValue().get(key);

        if (attempts == null) {
            return MAX_LOGIN_ATTEMPTS;
        }

        return Math.max(0, MAX_LOGIN_ATTEMPTS - Integer.parseInt(attempts));
    }

    /**
     * Reset login attempts after successful login
     */
    public void resetLoginAttempts(String email) {
        String key = LOGIN_ATTEMPTS_PREFIX + email;
        stringRedisTemplate.delete(key);
    }

    /**
     * Get time until rate limit resets (in seconds)
     */
    public long getRateLimitResetTime(String email) {
        String key = LOGIN_ATTEMPTS_PREFIX + email;
        Long ttl = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
        return ttl != null && ttl > 0 ? ttl : 0;
    }

    // ==================== USER SESSION MANAGEMENT ====================

    /**
     * Store user session data
     */
    public void storeUserSession(UUID userId, String sessionData, long ttlSeconds) {
        String key = USER_SESSION_PREFIX + userId.toString();
        stringRedisTemplate.opsForValue().set(key, sessionData, ttlSeconds, TimeUnit.SECONDS);
    }

    /**
     * Get user session data
     */
    public String getUserSession(UUID userId) {
        String key = USER_SESSION_PREFIX + userId.toString();
        return stringRedisTemplate.opsForValue().get(key);
    }

    /**
     * Delete user session
     */
    public void deleteUserSession(UUID userId) {
        String key = USER_SESSION_PREFIX + userId.toString();
        stringRedisTemplate.delete(key);
    }

    /**
     * Invalidate all tokens for a user (used when password is reset)
     * This includes all refresh tokens and sessions
     */
    public void invalidateAllUserTokens(UUID userId) {
        // Invalidate all refresh tokens
        invalidateAllRefreshTokens(userId);

        // Delete user session
        deleteUserSession(userId);

        log.info("Invalidated all tokens and sessions for user {}", userId);
    }
}
