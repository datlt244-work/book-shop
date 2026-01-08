package com.ecommerce.auth_service.client;

import com.ecommerce.auth_service.client.dto.CreateUserProfileRequest;
import com.ecommerce.auth_service.client.dto.UserBasicInfo;
import com.ecommerce.auth_service.client.dto.UserProfileInfo;
import com.ecommerce.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Fallback factory for UserServiceClient.
 * 
 * Provides fallback behavior when user-service is unavailable.
 */
@Slf4j
@Component
public class UserServiceClientFallback implements FallbackFactory<UserServiceClient> {

    @Override
    public UserServiceClient create(Throwable cause) {
        log.error("User service unavailable, using fallback. Cause: {}", cause.getMessage());
        
        return new UserServiceClient() {
            @Override
            public ApiResponse<UserProfileInfo> createUserProfile(CreateUserProfileRequest request) {
                log.error("Fallback: createUserProfile for user {}", request.getUserId());
                return ApiResponse.<UserProfileInfo>builder()
                        .code(503)
                        .message("User service unavailable")
                        .build();
            }

            @Override
            public ApiResponse<UserBasicInfo> getUserBasicInfo(UUID userId) {
                log.error("Fallback: getUserBasicInfo for user {}", userId);
                // Return null info - caller should handle gracefully
                return ApiResponse.<UserBasicInfo>builder()
                        .code(503)
                        .message("User service unavailable")
                        .result(null)
                        .build();
            }

            @Override
            public ApiResponse<UserProfileInfo> getUserProfile(UUID userId) {
                log.error("Fallback: getUserProfile for user {}", userId);
                return ApiResponse.<UserProfileInfo>builder()
                        .code(503)
                        .message("User service unavailable")
                        .result(null)
                        .build();
            }

            @Override
            public ApiResponse<Void> deleteUserProfile(UUID userId) {
                log.error("Fallback: deleteUserProfile for user {}", userId);
                return ApiResponse.<Void>builder()
                        .code(503)
                        .message("User service unavailable")
                        .build();
            }

            @Override
            public ApiResponse<Boolean> userExists(UUID userId) {
                log.error("Fallback: userExists for user {}", userId);
                return ApiResponse.<Boolean>builder()
                        .code(503)
                        .message("User service unavailable")
                        .result(false)
                        .build();
            }
        };
    }
}

