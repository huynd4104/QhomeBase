package com.QhomeBase.assetmaintenanceservice.dto.service;

import com.QhomeBase.assetmaintenanceservice.model.service.enums.ServiceBookingItemType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateServiceBookingItemRequest {

    @NotNull(message = "Item type is required")
    private ServiceBookingItemType itemType;

    @NotNull(message = "Item ID is required")
    private UUID itemId;

    @NotBlank(message = "Item code is required")
    @Size(max = 64, message = "Item code must not exceed 64 characters")
    private String itemCode;

    @NotBlank(message = "Item name is required")
    @Size(max = 255, message = "Item name must not exceed 255 characters")
    private String itemName;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Unit price must be positive")
    private BigDecimal unitPrice;

    @DecimalMin(value = "0.0", inclusive = false, message = "Total price must be positive")
    private BigDecimal totalPrice;

    private Map<String, Object> metadata;
}

