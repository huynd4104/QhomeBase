package com.QhomeBase.baseservice.dto;

import com.QhomeBase.baseservice.model.VehicleRegistrationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record VehicleRegistrationDto(
        UUID id,
        UUID vehicleId,
        String vehiclePlateNo,
        String vehicleKind,
        String vehicleColor,
        String reason,
        VehicleRegistrationStatus status,
        UUID requestedBy,
        String requestedByName,
        UUID approvedBy,
        String approvedByName,
        String note,
        OffsetDateTime requestedAt,
        OffsetDateTime approvedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}


