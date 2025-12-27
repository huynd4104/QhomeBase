package com.QhomeBase.assetmaintenanceservice.dto.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceComboItemDto {

    private UUID id;
    private UUID comboId;
    private String itemName;
    private String itemDescription;
    private BigDecimal itemPrice;
    private Integer itemDurationMinutes;
    private Integer quantity;
    private String note;
    private Integer sortOrder;
    private OffsetDateTime createdAt;
}

