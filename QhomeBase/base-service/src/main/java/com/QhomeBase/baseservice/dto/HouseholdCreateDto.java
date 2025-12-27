package com.QhomeBase.baseservice.dto;

import com.QhomeBase.baseservice.model.HouseholdKind;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record HouseholdCreateDto(
        @NotNull(message = "Unit ID is required")
        UUID unitId,
        
        @NotNull(message = "Household kind is required")
        HouseholdKind kind,

        UUID contractId,
        
        UUID primaryResidentId,
        
        @NotNull(message = "Start date is required")
        LocalDate startDate,
        
        LocalDate endDate
) {}



