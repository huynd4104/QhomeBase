package com.QhomeBase.marketplaceservice.dto;

import com.QhomeBase.marketplaceservice.validation.ValidContactInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidContactInfo
public class ContactInfoRequest {
    private String phone;
    private String email;
    @Builder.Default
    private Boolean showPhone = true;
    @Builder.Default
    private Boolean showEmail = false;
}

