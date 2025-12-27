package com.QhomeBase.baseservice.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MeterDto(
        UUID id,
        UUID unitId,
        UUID buildingId,
        String buildingCode,
        String unitCode,
        Integer floor,
        UUID serviceId,
        String serviceCode,
        String serviceName,
        String meterCode,
        Boolean active,
        LocalDate installedAt,
        LocalDate removedAt,
        Double lastReading,
        LocalDate lastReadingDate,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}

