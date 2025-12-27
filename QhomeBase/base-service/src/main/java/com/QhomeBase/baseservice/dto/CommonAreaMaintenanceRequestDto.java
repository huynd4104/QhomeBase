package com.QhomeBase.baseservice.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CommonAreaMaintenanceRequestDto(
        UUID id,
        UUID buildingId,
        UUID residentId,
        UUID userId,
        UUID createdBy,
        String areaType,
        String title,
        String description,
        List<String> attachments,
        String location,
        String contactName,
        String contactPhone,
        String note,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String adminResponse // Optional - admin can add notes when approve/deny/complete
) {}
