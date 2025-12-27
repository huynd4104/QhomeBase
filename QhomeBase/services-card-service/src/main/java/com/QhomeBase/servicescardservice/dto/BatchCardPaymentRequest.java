package com.QhomeBase.servicescardservice.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record BatchCardPaymentRequest(
        @NotNull(message = "Unit ID is required")
        UUID unitId,
        
        @NotEmpty(message = "At least one registration ID is required")
        List<@NotNull UUID> registrationIds
) {}

