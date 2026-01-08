package com.ecommerce.user.entity;

import com.ecommerce.common.entity.JpaBaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * User profile entity - stores profile information separate from auth credentials.
 * The user_id is the same UUID as auth-service's user_credentials.id
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_profiles", indexes = {
        @Index(name = "idx_user_profiles_email", columnList = "email"),
        @Index(name = "idx_user_profiles_full_name", columnList = "full_name"),
        @Index(name = "idx_user_profiles_created_at", columnList = "created_at")
})
public class UserProfile extends JpaBaseEntity {

    /**
     * Primary key - same UUID as auth-service user_credentials.id
     * This is NOT auto-generated, it's passed from auth-service during registration
     */
    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    /**
     * Email - copied from auth-service for convenience (display purposes)
     */
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    /**
     * Full name
     */
    @Column(name = "full_name", length = 100)
    private String fullName;

    /**
     * Phone number
     */
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    /**
     * Avatar URL (stored in MinIO)
     */
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    /**
     * Date of birth
     */
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    /**
     * Bio/description
     */
    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    /**
     * User addresses
     */
    @OneToMany(mappedBy = "userProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserAddress> addresses = new ArrayList<>();

    /**
     * User preferences
     */
    @OneToOne(mappedBy = "userProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserPreferences preferences;

    // Helper methods
    public void addAddress(UserAddress address) {
        addresses.add(address);
        address.setUserProfile(this);
    }

    public void removeAddress(UserAddress address) {
        addresses.remove(address);
        address.setUserProfile(null);
    }
}

