package com.QhomeBase.baseservice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MeterWithReadingDto(
        UUID id,
        UUID unitId,
        String unitCode,
        Integer floor,
        UUID serviceId,
        String serviceCode,
        String serviceName,
        String meterCode,
        Boolean active,
        LocalDate installedAt,
        LocalDate removedAt,
        BigDecimal prevIndex,
        BigDecimal currIndex,
        Boolean hasReading,
        UUID readingId,
        LocalDate readingDate,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}


