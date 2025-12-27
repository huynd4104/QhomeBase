package com.QhomeBase.baseservice.dto;

import com.QhomeBase.baseservice.model.InspectionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AssetInspectionDto(
        UUID id,
        UUID contractId,
        UUID unitId,
        String unitCode,
        LocalDate inspectionDate,
        LocalDate scheduledDate,
        InspectionStatus status,
        String inspectorName,
        UUID inspectorId,
        String inspectorNotes,
        OffsetDateTime completedAt,
        UUID completedBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<AssetInspectionItemDto> items,
        BigDecimal totalDamageCost,
        UUID invoiceId
) {}

