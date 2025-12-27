package com.QhomeBase.assetmaintenanceservice.dto.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateServiceComboRequest {

    @NotBlank(message = "Combo name is required")
    @Size(max = 255, message = "Combo name must not exceed 255 characters")
    private String name;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    private String servicesIncluded;

    private Integer durationMinutes;

    @NotNull(message = "Combo price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be positive")
    private BigDecimal price;

    private Boolean isActive;

    private Integer sortOrder;

    @Valid
    private List<ServiceComboItemRequest> items;
}

