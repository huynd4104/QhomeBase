package com.QhomeBase.baseservice.dto;

import com.QhomeBase.baseservice.model.AssetType;
import com.QhomeBase.baseservice.model.RoomType;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AssetDto(
        UUID id,
        UUID unitId,
        UUID buildingId,
        String buildingCode,
        String unitCode,
        Integer floor,
        AssetType assetType,
        RoomType roomType,
        String assetCode,
        String name,
        String brand,
        String model,
        String serialNumber,
        String description,
        Boolean active,
        LocalDate installedAt,
        LocalDate removedAt,
        LocalDate warrantyUntil,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
