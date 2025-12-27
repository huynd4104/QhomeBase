package com.QhomeBase.baseservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateContractProxyRequest(
        @NotNull(message = "Unit ID is required")
        UUID unitId,

        @NotBlank(message = "Contract number is required")
        String contractNumber,

        String contractType,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        LocalDate endDate,

        @PositiveOrZero(message = "Monthly rent must be positive or zero")
        BigDecimal monthlyRent,

        @PositiveOrZero(message = "Purchase price must be positive or zero")
        BigDecimal purchasePrice,

        String paymentMethod,

        String paymentTerms,

        LocalDate purchaseDate,

        String notes,

        String status
) {}




