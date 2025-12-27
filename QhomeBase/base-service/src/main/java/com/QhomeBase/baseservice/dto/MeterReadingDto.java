package com.QhomeBase.baseservice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MeterReadingDto(
        UUID id,
        UUID assignmentId,
        UUID cycleId,
        UUID meterId,
        String meterCode,
        UUID unitId,
        String unitCode,
        Integer floor,
        BigDecimal prevIndex,
        BigDecimal currentIndex,
        BigDecimal consumption,
        LocalDate readingDate,
        String note,
        UUID readerId,
        UUID photoFileId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
