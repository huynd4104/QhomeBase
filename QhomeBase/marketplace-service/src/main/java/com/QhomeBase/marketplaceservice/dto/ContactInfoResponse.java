package com.QhomeBase.marketplaceservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactInfoResponse {
    private String phone;
    private String phoneDisplay; // Masked phone number
    private String email;
    private Boolean showPhone;
    private Boolean showEmail;
}

