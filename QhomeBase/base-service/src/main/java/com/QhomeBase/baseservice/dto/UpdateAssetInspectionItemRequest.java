package com.QhomeBase.baseservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateAssetInspectionItemRequest(
        String conditionStatus,
        String notes,
        Boolean checked,
        UUID checkedBy,
        BigDecimal damageCost
) {}

