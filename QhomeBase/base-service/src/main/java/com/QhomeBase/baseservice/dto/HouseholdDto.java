package com.QhomeBase.baseservice.dto;

import com.QhomeBase.baseservice.model.HouseholdKind;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record HouseholdDto(
        UUID id,
        UUID unitId,
        String unitCode,
        HouseholdKind kind,
        UUID primaryResidentId,
        String primaryResidentName,
        LocalDate startDate,
        LocalDate endDate,
        UUID contractId,
        String contractNumber,
        LocalDate contractStartDate,
        LocalDate contractEndDate,
        String contractStatus,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}



