package com.QhomeBase.baseservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record UnitUpdateDto(

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
