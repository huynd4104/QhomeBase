package com.QhomeBase.servicescardservice.dto;

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
public class CardPricingDto {
    private UUID id;
    private String cardType; // VEHICLE, RESIDENT, ELEVATOR
    private BigDecimal price;
    private String currency;
    private String description;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private UUID createdBy;
    private OffsetDateTime updatedAt;
    private UUID updatedBy;
}

