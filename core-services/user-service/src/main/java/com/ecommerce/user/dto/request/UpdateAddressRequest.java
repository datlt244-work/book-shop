package com.ecommerce.user.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Request DTO for updating an address
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAddressRequest {

    @Size(max = 100)
    private String recipientName;

    @Size(max = 20)
    private String phone;

    private String provinceCode;
    private String provinceName;

    private String districtCode;
    private String districtName;

    private String wardCode;
    private String wardName;

    private String streetAddress;

    private String addressType;

    private Boolean isDefault;

    @Size(max = 50)
    private String label;
}

