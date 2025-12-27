package com.QhomeBase.assetmaintenanceservice.dto.service;

import com.QhomeBase.assetmaintenanceservice.model.service.enums.ServicePricingType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateServiceRequest {

    @NotNull(message = "Category ID is required")
    private UUID categoryId;

    @NotBlank(message = "Service code is required")
    @Size(max = 64, message = "Service code must not exceed 64 characters")
    private String code;

    @NotBlank(message = "Service name is required")
    @Size(max = 255, message = "Service name must not exceed 255 characters")
    private String name;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @Size(max = 255, message = "Location must not exceed 255 characters")
    private String location;

    @Size(max = 1024, message = "Map URL must not exceed 1024 characters")
    private String mapUrl;

    @DecimalMin(value = "0.0", inclusive = false, message = "Price per hour must be positive")
    private BigDecimal pricePerHour;

    @DecimalMin(value = "0.0", inclusive = false, message = "Price per session must be positive")
    private BigDecimal pricePerSession;

    @NotNull(message = "Pricing type is required")
    private ServicePricingType pricingType;

    @Min(value = 1, message = "Max capacity must be at least 1")
    @Max(value = 1000, message = "Max capacity must be <= 1000")
    private Integer maxCapacity;

    @Min(value = 1, message = "Min duration must be at least 1 hour")
    private Integer minDurationHours;

    private String rules;

    private Boolean isActive;
}

