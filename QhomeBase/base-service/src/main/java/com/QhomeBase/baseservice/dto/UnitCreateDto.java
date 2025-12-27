package com.QhomeBase.baseservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record UnitCreateDto(
        @NotNull(message = "Building ID is required")
        UUID buildingId,

        @Size(max = 50, message = "Code must not exceed 50 characters")
        String code,

        @NotNull(message = "Floor is required")
        @Positive(message = "Floor must be positive")
        Integer floor,

        @NotNull(message = "Area is required")
        @Positive(message = "Area must be positive")
        BigDecimal areaM2,

        @NotNull(message = "Bedrooms is required")
        @Positive(message = "Bedrooms must be positive")
        Integer bedrooms
) {}
