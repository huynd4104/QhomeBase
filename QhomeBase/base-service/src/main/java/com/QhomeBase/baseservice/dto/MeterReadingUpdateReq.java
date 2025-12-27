package com.QhomeBase.baseservice.dto;

import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record MeterReadingUpdateReq(
        LocalDate readingDate,
        @PositiveOrZero BigDecimal prevIndex,
        @PositiveOrZero BigDecimal currIndex,
        UUID photoFileId,
        String note
) {}

