package com.ecommerce.user.service;

import com.ecommerce.common.exception.AppException;
import com.ecommerce.common.exception.ErrorCode;
import com.ecommerce.common.service.MinioService;
import com.ecommerce.user.dto.request.*;
import com.ecommerce.user.dto.response.*;
import com.ecommerce.user.entity.UserAddress;
import com.ecommerce.user.entity.UserPreferences;
import com.ecommerce.user.entity.UserProfile;
import com.ecommerce.user.repository.UserAddressRepository;
import com.ecommerce.user.repository.UserPreferencesRepository;
import com.ecommerce.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserProfileRepository userProfileRepository;
    private final UserAddressRepository userAddressRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    
    // Optional - may be null if MinIO is not configured
    @Autowired(required = false)
    private MinioService minioService;

    // ==================== PROFILE OPERATIONS ====================

    /**
     * Create a new user profile (called from auth-service during registration)
     */
    @Transactional
    public UserProfileResponse createProfile(CreateProfileRequest request) {
        // Check if profile already exists
        if (userProfileRepository.existsById(request.getUserId())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        UserProfile profile = UserProfile.builder()
                .userId(request.getUserId())
                .email(request.getEmail())
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .build();

        // Create default preferences
        UserPreferences preferences = UserPreferences.builder()
                .userProfile(profile)
                .build();
        profile.setPreferences(preferences);

        UserProfile savedProfile = userProfileRepository.save(profile);
        log.info("Created profile for user: {}", savedProfile.getUserId());

        return mapToProfileResponse(savedProfile);
    }

    /**
     * Get user profile by ID
     */
    public UserProfileResponse getProfile(UUID userId) {
        UserProfile profile = userProfileRepository.findByIdWithAddresses(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return mapToProfileResponse(profile);
    }

    /**
     * Get basic user info (for service-to-service calls)
     */
    public UserBasicInfoResponse getBasicInfo(UUID userId) {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Generate presigned URL for avatar (handle MinIO errors gracefully)
        String avatarUrl = null;
        if (profile.getAvatarUrl() != null && minioService != null) {
            try {
                avatarUrl = minioService.getPresignedUrl(profile.getAvatarUrl());
            } catch (Exception e) {
                log.warn("Could not generate presigned URL for avatar: {}", e.getMessage());
                avatarUrl = profile.getAvatarUrl(); // Fallback to raw URL
            }
        }

        return UserBasicInfoResponse.builder()
                .userId(profile.getUserId())
                .email(profile.getEmail())
                .fullName(profile.getFullName())
                .phoneNumber(profile.getPhoneNumber())
                .avatarUrl(avatarUrl)
                .build();
    }

    /**
     * Update user profile
     */
    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Update only provided fields
        if (request.getFullName() != null) {
            profile.setFullName(request.getFullName());
        }
        if (request.getPhoneNumber() != null) {
            profile.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getAvatarUrl() != null) {
            profile.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getDateOfBirth() != null) {
            profile.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getBio() != null) {
            profile.setBio(request.getBio());
        }

        UserProfile savedProfile = userProfileRepository.save(profile);
        log.info("Updated profile for user: {}", userId);

        return mapToProfileResponse(savedProfile);
    }

    /**
     * Delete user profile (called when user account is deleted)
     */
    @Transactional
    public void deleteProfile(UUID userId) {
        if (!userProfileRepository.existsById(userId)) {
            throw new AppException(ErrorCode.USER_NOT_EXISTED);
        }

        userProfileRepository.deleteById(userId);
        log.info("Deleted profile for user: {}", userId);
    }

    // ==================== ADDRESS OPERATIONS ====================

    /**
     * Get all addresses for a user
     */
    public List<AddressResponse> getAddresses(UUID userId) {
        List<UserAddress> addresses = userAddressRepository.findByUserProfileUserId(userId);
        return addresses.stream()
                .map(this::mapToAddressResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific address
     */
    public AddressResponse getAddress(UUID userId, UUID addressId) {
        UserAddress address = userAddressRepository.findByIdAndUserProfileUserId(addressId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.ADDRESS_NOT_FOUND));
        return mapToAddressResponse(address);
    }

    /**
     * Create a new address
     */
    @Transactional
    public AddressResponse createAddress(UUID userId, CreateAddressRequest request) {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        UserAddress address = UserAddress.builder()
                .userProfile(profile)
                .recipientName(request.getRecipientName())
                .phone(request.getPhone())
                .provinceCode(request.getProvinceCode())
                .provinceName(request.getProvinceName())
                .districtCode(request.getDistrictCode())
                .districtName(request.getDistrictName())
                .wardCode(request.getWardCode())
                .wardName(request.getWardName())
                .streetAddress(request.getStreetAddress())
                .addressType(request.getAddressType())
                .isDefault(request.getIsDefault())
                .label(request.getLabel())
                .build();

        // If this is set as default, clear other defaults
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            userAddressRepository.findByUserProfileUserIdAndIsDefaultTrue(userId)
                    .ifPresent(defaultAddr -> {
                        defaultAddr.setIsDefault(false);
                        userAddressRepository.save(defaultAddr);
                    });
        }

        // If this is the first address, make it default
        if (userAddressRepository.countByUserProfileUserId(userId) == 0) {
            address.setIsDefault(true);
        }

        UserAddress savedAddress = userAddressRepository.save(address);
        log.info("Created address {} for user: {}", savedAddress.getId(), userId);

        return mapToAddressResponse(savedAddress);
    }

    /**
     * Update an address
     */
    @Transactional
    public AddressResponse updateAddress(UUID userId, UUID addressId, UpdateAddressRequest request) {
        UserAddress address = userAddressRepository.findByIdAndUserProfileUserId(addressId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.ADDRESS_NOT_FOUND));

        // Update only provided fields
        if (request.getRecipientName() != null) address.setRecipientName(request.getRecipientName());
        if (request.getPhone() != null) address.setPhone(request.getPhone());
        if (request.getProvinceCode() != null) address.setProvinceCode(request.getProvinceCode());
        if (request.getProvinceName() != null) address.setProvinceName(request.getProvinceName());
        if (request.getDistrictCode() != null) address.setDistrictCode(request.getDistrictCode());
        if (request.getDistrictName() != null) address.setDistrictName(request.getDistrictName());
        if (request.getWardCode() != null) address.setWardCode(request.getWardCode());
        if (request.getWardName() != null) address.setWardName(request.getWardName());
        if (request.getStreetAddress() != null) address.setStreetAddress(request.getStreetAddress());
        if (request.getAddressType() != null) address.setAddressType(request.getAddressType());
        if (request.getLabel() != null) address.setLabel(request.getLabel());

        // Handle default flag
        if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(address.getIsDefault())) {
            userAddressRepository.clearOtherDefaultAddresses(userId, addressId);
            address.setIsDefault(true);
        }

        UserAddress savedAddress = userAddressRepository.save(address);
        log.info("Updated address {} for user: {}", addressId, userId);

        return mapToAddressResponse(savedAddress);
    }

    /**
     * Delete an address
     */
    @Transactional
    public void deleteAddress(UUID userId, UUID addressId) {
        UserAddress address = userAddressRepository.findByIdAndUserProfileUserId(addressId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.ADDRESS_NOT_FOUND));

        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());
        userAddressRepository.delete(address);

        // If deleted address was default, set another one as default
        if (wasDefault) {
            userAddressRepository.findByUserProfileUserId(userId).stream()
                    .findFirst()
                    .ifPresent(newDefault -> {
                        newDefault.setIsDefault(true);
                        userAddressRepository.save(newDefault);
                    });
        }

        log.info("Deleted address {} for user: {}", addressId, userId);
    }

    // ==================== MAPPER METHODS ====================

    private UserProfileResponse mapToProfileResponse(UserProfile profile) {
        // Generate presigned URL for avatar (handle MinIO errors gracefully)
        String avatarUrl = null;
        if (profile.getAvatarUrl() != null && minioService != null) {
            try {
                avatarUrl = minioService.getPresignedUrl(profile.getAvatarUrl());
            } catch (Exception e) {
                log.warn("Could not generate presigned URL: {}", e.getMessage());
                avatarUrl = profile.getAvatarUrl();
            }
        }

        List<AddressResponse> addresses = profile.getAddresses() != null
                ? profile.getAddresses().stream().map(this::mapToAddressResponse).collect(Collectors.toList())
                : null;

        UserPreferencesResponse preferencesResponse = profile.getPreferences() != null
                ? mapToPreferencesResponse(profile.getPreferences())
                : null;

        return UserProfileResponse.builder()
                .userId(profile.getUserId())
                .email(profile.getEmail())
                .fullName(profile.getFullName())
                .phoneNumber(profile.getPhoneNumber())
                .avatarUrl(avatarUrl)
                .dateOfBirth(profile.getDateOfBirth())
                .bio(profile.getBio())
                .addresses(addresses)
                .preferences(preferencesResponse)
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    private AddressResponse mapToAddressResponse(UserAddress address) {
        return AddressResponse.builder()
                .id(address.getId())
                .recipientName(address.getRecipientName())
                .phone(address.getPhone())
                .provinceCode(address.getProvinceCode())
                .provinceName(address.getProvinceName())
                .districtCode(address.getDistrictCode())
                .districtName(address.getDistrictName())
                .wardCode(address.getWardCode())
                .wardName(address.getWardName())
                .streetAddress(address.getStreetAddress())
                .fullAddress(address.getFullAddress())
                .addressType(address.getAddressType())
                .isDefault(address.getIsDefault())
                .label(address.getLabel())
                .createdAt(address.getCreatedAt())
                .updatedAt(address.getUpdatedAt())
                .build();
    }

    private UserPreferencesResponse mapToPreferencesResponse(UserPreferences prefs) {
        return UserPreferencesResponse.builder()
                .emailNotifications(prefs.getEmailNotifications())
                .smsNotifications(prefs.getSmsNotifications())
                .pushNotifications(prefs.getPushNotifications())
                .marketingEmails(prefs.getMarketingEmails())
                .newsletter(prefs.getNewsletter())
                .language(prefs.getLanguage())
                .currency(prefs.getCurrency())
                .theme(prefs.getTheme())
                .build();
    }
}

