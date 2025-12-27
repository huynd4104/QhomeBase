package com.QhomeBase.assetmaintenanceservice.dto.service;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateServiceBookingItemRequest {

    @Size(max = 255, message = "Item name must not exceed 255 characters")
    private String itemName;

    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @DecimalMin(value = "0.0", inclusive = false, message = "Unit price must be positive")
    private BigDecimal unitPrice;

    @DecimalMin(value = "0.0", inclusive = false, message = "Total price must be positive")
    private BigDecimal totalPrice;

    private Map<String, Object> metadata;
}

