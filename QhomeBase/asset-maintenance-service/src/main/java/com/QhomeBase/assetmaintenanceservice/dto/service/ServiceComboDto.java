package com.QhomeBase.assetmaintenanceservice.dto.service;

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
public class ServiceComboDto {

    private UUID id;
    private UUID serviceId;
    private String code;
    private String name;
    private String description;
    private String servicesIncluded;
    private Integer durationMinutes;
    private BigDecimal price;
    private Boolean isActive;
    private Integer sortOrder;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<ServiceComboItemDto> items;
}

