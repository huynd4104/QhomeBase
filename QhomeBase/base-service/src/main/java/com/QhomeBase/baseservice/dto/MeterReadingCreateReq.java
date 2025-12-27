package com.QhomeBase.baseservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record MeterReadingCreateReq(
        @NotNull UUID meterId,
        UUID assignmentId,
        UUID cycleId,
        @NotNull LocalDate readingDate,
        @PositiveOrZero BigDecimal prevIndex,
        @NotNull @PositiveOrZero BigDecimal currIndex,
        UUID photoFileId,
        String note,
        UUID readerId
) {}

