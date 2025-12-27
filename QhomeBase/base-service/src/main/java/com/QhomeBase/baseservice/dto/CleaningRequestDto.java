package com.QhomeBase.baseservice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CleaningRequestDto(
        UUID id,
        UUID unitId,
        UUID residentId,
        UUID createdBy,
        UUID userId,
        String cleaningType,
        LocalDate cleaningDate,
        LocalTime startTime,
        BigDecimal durationHours,
        String location,
        String note,
        String contactPhone,
        List<String> extraServices,
        String paymentMethod,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime lastResentAt,
        boolean resendAlertSent
) {
}


