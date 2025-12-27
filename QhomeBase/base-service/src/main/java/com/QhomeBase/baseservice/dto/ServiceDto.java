package com.QhomeBase.baseservice.dto;

import com.QhomeBase.baseservice.model.ServiceType;
import com.QhomeBase.baseservice.model.ServiceUnit;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ServiceDto(
        UUID id,
        String code,
        String name,
        String nameEn,
        ServiceType type,
        ServiceUnit unit,
        String unitLabel,
        Boolean billable,
        Boolean requiresMeter,
        Boolean active,
        String description,
        Integer displayOrder,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}

