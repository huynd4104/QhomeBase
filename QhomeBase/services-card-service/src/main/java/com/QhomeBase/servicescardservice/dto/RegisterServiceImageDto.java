package com.QhomeBase.servicescardservice.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RegisterServiceImageDto(
        UUID id,
        UUID registerVehicleId,
        String imageUrl,
        OffsetDateTime createdAt
) {}

