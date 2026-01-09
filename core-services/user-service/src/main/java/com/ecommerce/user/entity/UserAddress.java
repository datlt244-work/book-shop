package com.ecommerce.user.entity;

import com.ecommerce.common.entity.JpaBaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * User address entity for shipping/billing addresses.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_addresses", indexes = {
        @Index(name = "idx_addresses_user_id", columnList = "user_id"),
        @Index(name = "idx_addresses_is_default", columnList = "is_default"),
        @Index(name = "idx_addresses_province", columnList = "province_code")
})
public class UserAddress extends JpaBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserProfile userProfile;

    /**
     * Recipient name for this address
     */
    @Column(name = "recipient_name", nullable = false, length = 100)
    private String recipientName;

    /**
     * Phone number for this address
     */
    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    /**
     * Province/City code (for Vietnam address system)
     */
    @Column(name = "province_code", nullable = false, length = 20)
    private String provinceCode;

    @Column(name = "province_name", length = 100)
    private String provinceName;

    /**
     * District code
     */
    @Column(name = "district_code", nullable = false, length = 20)
    private String districtCode;

    @Column(name = "district_name", length = 100)
    private String districtName;

    /**
     * Ward code
     */
    @Column(name = "ward_code", length = 20)
    private String wardCode;

    @Column(name = "ward_name", length = 100)
    private String wardName;

    /**
     * Street address, house number, etc.
     */
    @Column(name = "street_address", nullable = false, columnDefinition = "TEXT")
    private String streetAddress;

    /**
     * Address type: shipping, billing, or both
     */
    @Column(name = "address_type", length = 20)
    @Builder.Default
    private String addressType = "shipping";

    /**
     * Is this the default address for the user
     */
    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    /**
     * Label for the address (Home, Office, etc.)
     */
    @Column(name = "label", length = 50)
    private String label;

    /**
     * Get full address as string
     */
    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        sb.append(streetAddress);
        if (wardName != null) sb.append(", ").append(wardName);
        if (districtName != null) sb.append(", ").append(districtName);
        if (provinceName != null) sb.append(", ").append(provinceName);
        return sb.toString();
    }
}

