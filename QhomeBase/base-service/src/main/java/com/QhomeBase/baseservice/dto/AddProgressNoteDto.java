package com.QhomeBase.baseservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record AddProgressNoteDto(
        @NotBlank(message = "Progress note is required")
        String note,
        
        // Optional: Update cost if provided (if null, keep original estimated cost)
        @PositiveOrZero(message = "Cost must be positive or zero")
        BigDecimal cost
) {}

