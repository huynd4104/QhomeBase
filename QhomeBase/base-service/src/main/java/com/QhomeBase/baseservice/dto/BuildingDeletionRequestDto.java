package com.QhomeBase.baseservice.dto;

import com.QhomeBase.baseservice.model.BuildingDeletionStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BuildingDeletionRequestDto(
        UUID id,
        UUID buildingId,
        UUID requestedBy,
        String reason,
        UUID approvedBy,
        String note,
        BuildingDeletionStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime approvedAt
) {}