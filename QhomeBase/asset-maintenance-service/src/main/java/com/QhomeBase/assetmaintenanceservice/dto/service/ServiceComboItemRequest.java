package com.QhomeBase.assetmaintenanceservice.dto.service;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
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
public class ServiceComboItemRequest {

    @NotBlank(message = "Item name is required")
    private String itemName;

    private String itemDescription;

    @DecimalMin(value = "0.0", message = "Item price must be greater or equal to 0")
    private BigDecimal itemPrice;

    @Min(value = 0, message = "Duration must be greater or equal to 0")
    private Integer itemDurationMinutes;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be greater than 0")
    private Integer quantity;

    private String note;

    private Integer sortOrder;
}

