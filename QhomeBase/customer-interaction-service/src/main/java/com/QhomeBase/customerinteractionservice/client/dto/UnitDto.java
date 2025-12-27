package com.QhomeBase.customerinteractionservice.client.dto;

import java.util.UUID;

public record UnitDto(
        UUID id,
        UUID buildingId,
        String buildingCode,
        String buildingName
) {
}




