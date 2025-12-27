package com.QhomeBase.servicescardservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCardPricingRequest {
    
    @NotBlank(message = "Card type is required")
    private String cardType; // VEHICLE, RESIDENT, ELEVATOR

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;

    private String currency; // Default: VND

    private String description;

    private Boolean isActive; // Default: true
}

