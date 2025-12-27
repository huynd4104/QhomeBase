package com.QhomeBase.assetmaintenanceservice.client.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UnitDto(
        UUID id,
        UUID buildingId,
        String buildingCode,
        String buildingName,
        String code,
        Integer floor,
        BigDecimal areaM2,
        Integer bedrooms,
        String status,
        UUID primaryResidentId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}










