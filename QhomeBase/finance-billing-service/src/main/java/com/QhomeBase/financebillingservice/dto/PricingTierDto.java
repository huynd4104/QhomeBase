package com.QhomeBase.financebillingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingTierDto {
    private UUID id;
    private String serviceCode;
    private Integer tierOrder;
    private BigDecimal minQuantity;
    private BigDecimal maxQuantity;
    private BigDecimal unitPrice;
    private LocalDate effectiveFrom;
    private LocalDate effectiveUntil;
    private Boolean active;
    private String description;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}

