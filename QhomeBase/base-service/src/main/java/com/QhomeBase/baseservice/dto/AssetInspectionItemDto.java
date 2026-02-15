package com.QhomeBase.baseservice.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AssetInspectionItemDto(
        UUID id,
        UUID assetId,
        String assetCode,
        String assetName,
        String assetType,
        String conditionStatus,
        String notes,
        Boolean checked,
        OffsetDateTime checkedAt,
        UUID checkedBy,
        BigDecimal damageCost
) {}

