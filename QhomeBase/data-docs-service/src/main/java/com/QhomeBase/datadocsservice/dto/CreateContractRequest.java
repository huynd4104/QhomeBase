package com.QhomeBase.datadocsservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateContractRequest {

    @NotNull(message = "Unit ID is required")
    private UUID unitId;

    @NotBlank(message = "Contract number is required")
    private String contractNumber;

    private String contractType; // RENTAL, PURCHASE, etc.

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    private LocalDate endDate;

    @PositiveOrZero(message = "Monthly rent must be positive or zero")
    private BigDecimal monthlyRent;

    @PositiveOrZero(message = "Purchase price must be positive or zero")
    private BigDecimal purchasePrice;

    private String paymentMethod;

    private String paymentTerms;

    private LocalDate purchaseDate;

    private String notes;

    private String status; // ACTIVE, EXPIRED, TERMINATED
}

