package com.QhomeBase.baseservice.dto;

import com.QhomeBase.baseservice.model.HouseholdKind;

import java.time.LocalDate;
import java.util.UUID;

public record HouseholdUpdateDto(
        UUID unitId,
        HouseholdKind kind,
        UUID contractId,
        UUID primaryResidentId,
        LocalDate startDate,
        LocalDate endDate
) {}



