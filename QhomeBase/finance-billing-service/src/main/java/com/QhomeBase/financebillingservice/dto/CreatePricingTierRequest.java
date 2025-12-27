package com.QhomeBase.financebillingservice.dto;

import jakarta.validation.constraints.*;
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
public class CreatePricingTierRequest {
    
    @NotBlank(message = "Service code is required")
    @Size(max = 50, message = "Service code must not exceed 50 characters")
    private String serviceCode;
    
    @NotNull(message = "Tier order is required")
    @Min(value = 1, message = "Tier order must be at least 1")
    private Integer tierOrder;
    
    @NotNull(message = "Min quantity is required")
    @DecimalMin(value = "0", message = "Min quantity must be >= 0")
    private BigDecimal minQuantity;
    
    @DecimalMin(value = "0", message = "Max quantity must be >= 0")
    private BigDecimal maxQuantity;
    
    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0", message = "Unit price must be >= 0")
    private BigDecimal unitPrice;
    
    @NotNull(message = "Effective from date is required")
    private LocalDate effectiveFrom;
    
    private LocalDate effectiveUntil;
    
    @Builder.Default
    private Boolean active = true;
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
}

