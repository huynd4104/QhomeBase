package com.QhomeBase.financebillingservice.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ReadingCycleDto(
        UUID id,
        String name,
        LocalDate periodFrom,
        LocalDate periodTo,
        String status,
        String description,
        UUID serviceId,
        String serviceCode,
        String serviceName,
        UUID createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}

