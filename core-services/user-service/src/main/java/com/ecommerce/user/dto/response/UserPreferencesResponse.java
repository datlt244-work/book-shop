package com.ecommerce.user.dto.response;

import lombok.*;

/**
 * Response DTO for user preferences
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferencesResponse {

    // Notification Preferences
    private Boolean emailNotifications;
    private Boolean smsNotifications;
    private Boolean pushNotifications;

    // Marketing Preferences
    private Boolean marketingEmails;
    private Boolean newsletter;

    // Display Preferences
    private String language;
    private String currency;
    private String theme;
}

