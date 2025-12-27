package com.QhomeBase.datadocsservice.dto;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateContractRequest {

    private String contractNumber;

    private String contractType;

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

    private String status;
}

