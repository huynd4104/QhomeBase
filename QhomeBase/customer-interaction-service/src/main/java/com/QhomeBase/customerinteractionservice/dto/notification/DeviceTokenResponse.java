package com.QhomeBase.customerinteractionservice.dto.notification;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class DeviceTokenResponse {
    UUID id;
    String token;
    String platform;
    String appVersion;
    Instant lastSeenAt;
    Instant updatedAt;
}

