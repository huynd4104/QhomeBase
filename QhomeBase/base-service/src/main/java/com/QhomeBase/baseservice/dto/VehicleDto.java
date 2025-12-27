package com.QhomeBase.baseservice.dto;

import com.QhomeBase.baseservice.model.VehicleKind;

import java.time.OffsetDateTime;
import java.util.UUID;

public record VehicleDto(
        UUID id,
        UUID residentId,
        String residentName,
        UUID unitId,
        String unitCode,
        String plateNo,
        VehicleKind kind,
        String color,
        Boolean active,
        OffsetDateTime activatedAt,
        OffsetDateTime registrationApprovedAt,
        UUID approvedBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}


