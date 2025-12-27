package com.QhomeBase.assetmaintenanceservice.client.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ResidentDto(
        UUID id,
        String fullName,
        String phone,
        String email,
        String nationalId,
        LocalDate dob,
        String status,
        UUID userId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
