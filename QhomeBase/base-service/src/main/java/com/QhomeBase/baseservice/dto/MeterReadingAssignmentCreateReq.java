package com.QhomeBase.baseservice.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record MeterReadingAssignmentCreateReq(
        @NotNull UUID cycleId,
        UUID buildingId,
        @NotNull UUID serviceId,
        @NotNull UUID assignedTo,
        LocalDate startDate,
        LocalDate endDate,
        String note,
        Integer floor,
        List<UUID> unitIds
) {}

