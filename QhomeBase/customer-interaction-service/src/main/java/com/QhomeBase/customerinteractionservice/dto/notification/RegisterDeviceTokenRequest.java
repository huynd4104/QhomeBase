package com.QhomeBase.customerinteractionservice.dto.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterDeviceTokenRequest {

    UUID residentId;
    UUID buildingId;
    UUID userId;

    @Size(max = 100)
    String role;

    @NotBlank
    @Size(max = 255)
    String token;

    @Size(max = 40)
    String platform;

    @Size(max = 40)
    String appVersion;
}

