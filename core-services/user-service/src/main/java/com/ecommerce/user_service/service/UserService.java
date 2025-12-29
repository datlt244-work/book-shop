package com.ecommerce.user_service.service;

import com.ecommerce.common.exception.AppException;
import com.ecommerce.common.exception.ErrorCode;
import com.ecommerce.user_service.dto.request.CreateUserRequest;
import com.ecommerce.user_service.dto.request.UpdateUserRequest;
import com.ecommerce.user_service.dto.response.UserAuthResponse;
import com.ecommerce.user_service.dto.response.UserResponse;
import com.ecommerce.user_service.entity.User;
import com.ecommerce.user_service.entity.UserRole;
import com.ecommerce.user_service.entity.UserStatus;
import com.ecommerce.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {
    
    private final UserRepository userRepository;
    
    // ==================== PUBLIC API (for external services) ====================
    
    /**
     * Get user by ID (public info only)
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(Integer userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        return mapToUserResponse(user);
    }
    
    /**
     * Get user by email (public info only)
     */
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        return mapToUserResponse(user);
    }
    
    /**
     * Check if email exists
     */
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    /**
     * Update user profile
     */
    public UserResponse updateUser(Integer userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        
        User savedUser = userRepository.save(user);
        log.info("Updated user profile for userId: {}", userId);
        return mapToUserResponse(savedUser);
    }
    
    // ==================== INTERNAL API (for auth-service) ====================
    
    /**
     * Get user with auth info (includes passwordHash) - INTERNAL USE ONLY
     */
    @Transactional(readOnly = true)
    public UserAuthResponse getUserAuthByEmail(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        return mapToUserAuthResponse(user);
    }
    
    /**
     * Get user with auth info by ID - INTERNAL USE ONLY
     */
    @Transactional(readOnly = true)
    public UserAuthResponse getUserAuthById(Integer userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        return mapToUserAuthResponse(user);
    }
    
    /**
     * Create new user (called from auth-service during registration)
     */
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        
        User user = User.builder()
            .email(request.getEmail())
            .passwordHash(request.getPasswordHash())
            .fullName(request.getFullName())
            .phoneNumber(request.getPhoneNumber())
            .role(UserRole.customer)
            .status(UserStatus.pending_verification)
            .emailVerified(false)
            .loginCount(0)
            .build();
        
        User savedUser = userRepository.save(user);
        log.info("Created new user: {}", savedUser.getEmail());
        return mapToUserResponse(savedUser);
    }
    
    /**
     * Update user password (called from auth-service)
     */
    public void updatePassword(Integer userId, String newPasswordHash) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        
        user.setPasswordHash(newPasswordHash);
        userRepository.save(user);
        log.info("Password updated for userId: {}", userId);
    }
    
    /**
     * Update email verification status
     */
    public void verifyEmail(Integer userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(LocalDateTime.now());
        user.setStatus(UserStatus.active);
        userRepository.save(user);
        log.info("Email verified for userId: {}", userId);
    }
    
    /**
     * Update login tracking info
     */
    public void updateLoginInfo(Integer userId, String ipAddress) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(ipAddress);
        user.setLoginCount(user.getLoginCount() + 1);
        userRepository.save(user);
        log.debug("Login info updated for userId: {}", userId);
    }
    
    // ==================== MAPPERS ====================
    
    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .phoneNumber(user.getPhoneNumber())
            .avatarUrl(user.getAvatarUrl())
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
    
    private UserAuthResponse mapToUserAuthResponse(User user) {
        return UserAuthResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .passwordHash(user.getPasswordHash())
            .role(user.getRole().name())
            .status(user.getStatus().name())
            .emailVerified(user.getEmailVerified())
            .build();
    }
}
