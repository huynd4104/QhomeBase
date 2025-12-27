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
public class UpdatePricingTierRequest {
    
    @Min(value = 1, message = "Tier order must be at least 1")
    private Integer tierOrder;
    
    @DecimalMin(value = "0", message = "Min quantity must be >= 0")
    private BigDecimal minQuantity;
    
    @DecimalMin(value = "0", message = "Max quantity must be >= 0")
    private BigDecimal maxQuantity;
    
    @DecimalMin(value = "0", message = "Unit price must be >= 0")
    private BigDecimal unitPrice;
    
    private LocalDate effectiveFrom;
    
    private LocalDate effectiveUntil;
    
    private Boolean active;
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
}

