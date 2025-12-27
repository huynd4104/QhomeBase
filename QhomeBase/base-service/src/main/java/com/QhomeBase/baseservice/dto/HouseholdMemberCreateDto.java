package com.QhomeBase.baseservice.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record HouseholdMemberCreateDto(
        @NotNull(message = "Household ID is required")
        UUID householdId,
        
        @NotNull(message = "Resident ID is required")
        UUID residentId,
        
        String relation,
        
        String proofOfRelationImageUrl,
        
        Boolean isPrimary,
        
        LocalDate joinedAt
) {}

