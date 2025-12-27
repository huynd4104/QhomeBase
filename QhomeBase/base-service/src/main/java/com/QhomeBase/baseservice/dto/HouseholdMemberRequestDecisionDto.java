package com.QhomeBase.baseservice.dto;

import jakarta.validation.constraints.NotNull;

public record HouseholdMemberRequestDecisionDto(
        @NotNull(message = "approve flag is required")
        Boolean approve,

        String rejectionReason
) {}





