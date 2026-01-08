package com.ecommerce.user.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for address
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponse {

    private UUID id;
    private String recipientName;
    private String phone;
    private String provinceCode;
    private String provinceName;
    private String districtCode;
    private String districtName;
    private String wardCode;
    private String wardName;
    private String streetAddress;
    private String fullAddress;
    private String addressType;
    private Boolean isDefault;
    private String label;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

