package com.QhomeBase.baseservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AdminMaintenanceResponseDto(
        @NotBlank(message = "Admin response is required")
        String adminResponse,
        
        @DecimalMin(value = "0.0", message = "Estimated cost must be non-negative")
        BigDecimal estimatedCost,
        
        String note,
        
        OffsetDateTime preferredDatetime
) {
}
