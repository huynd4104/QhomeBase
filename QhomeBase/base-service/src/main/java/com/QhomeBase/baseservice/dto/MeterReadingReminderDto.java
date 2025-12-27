package com.QhomeBase.baseservice.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MeterReadingReminderDto(
        UUID id,
        String title,
        String message,
        LocalDate dueDate,
        OffsetDateTime createdAt,
        OffsetDateTime acknowledgedAt,
        UUID assignmentId,
        UUID cycleId,
        String cycleName,
        UUID buildingId,
        String type
) {}








