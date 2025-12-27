package com.QhomeBase.baseservice.dto;

import com.QhomeBase.baseservice.model.ReadingCycleStatus;

import java.time.LocalDate;
import java.util.UUID;

public record ReadingCycleProgressDto(
        UUID cycleId,
        String cycleName,
        LocalDate periodFrom,
        LocalDate periodTo,
        ReadingCycleStatus status,
        Long totalAssignments,
        Long assignmentsInProgress,
        Long assignmentsCompleted,
        Long totalReadings,
        Long readingsVerified,
        Integer progressPercentage
) {}

