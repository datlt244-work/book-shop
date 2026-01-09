package com.ecommerce.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Request DTO for creating a new address
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAddressRequest {

    @NotBlank(message = "Recipient name is required")
    @Size(max = 100)
    private String recipientName;

    @NotBlank(message = "Phone is required")
    @Size(max = 20)
    private String phone;

    @NotBlank(message = "Province code is required")
    private String provinceCode;

    private String provinceName;

    @NotBlank(message = "District code is required")
    private String districtCode;

    private String districtName;

    private String wardCode;

    private String wardName;

    @NotBlank(message = "Street address is required")
    private String streetAddress;

    private String addressType = "shipping";

    private Boolean isDefault = false;

    @Size(max = 50)
    private String label;
}

