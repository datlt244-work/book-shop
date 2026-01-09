package com.ecommerce.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User preferences for notifications and display settings.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_preferences")
public class UserPreferences {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private UserProfile userProfile;

    // Notification Preferences
    @Column(name = "email_notifications")
    @Builder.Default
    private Boolean emailNotifications = true;

    @Column(name = "sms_notifications")
    @Builder.Default
    private Boolean smsNotifications = false;

    @Column(name = "push_notifications")
    @Builder.Default
    private Boolean pushNotifications = true;

    // Marketing Preferences
    @Column(name = "marketing_emails")
    @Builder.Default
    private Boolean marketingEmails = false;

    @Column(name = "newsletter")
    @Builder.Default
    private Boolean newsletter = false;

    // Display Preferences
    @Column(name = "language", length = 10)
    @Builder.Default
    private String language = "vi";

    @Column(name = "currency", length = 10)
    @Builder.Default
    private String currency = "VND";

    @Column(name = "theme", length = 20)
    @Builder.Default
    private String theme = "light";

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

