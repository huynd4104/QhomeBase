package com.QhomeBase.assetmaintenanceservice.dto.service;

import com.QhomeBase.assetmaintenanceservice.model.service.enums.ServicePricingType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResidentServiceDetailDto {

    private UUID id;
    private String code;
    private String name;
    private String description;
    private String location;
    private String mapUrl;
    private BigDecimal pricePerHour;
    private BigDecimal pricePerSession;
    private ServicePricingType pricingType;
    private Integer maxCapacity;
    private Integer minDurationHours;
    private String rules;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private ServiceCategoryDto category;
    private List<ServiceAvailabilityDto> availabilities;
    private List<ServiceComboDto> combos;
    private List<ServiceOptionDto> options;
    private List<ServiceTicketDto> tickets;
}

