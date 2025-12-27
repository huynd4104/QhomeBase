package com.QhomeBase.assetmaintenanceservice.dto.asset;

import com.QhomeBase.assetmaintenanceservice.model.AssetStatus;
import com.QhomeBase.assetmaintenanceservice.model.AssetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetResponse {

    private UUID id;

    private UUID buildingId;

    private UUID unitId;

    private String code;

    private String name;

    private AssetType assetType;

    private AssetStatus status;

    private String location;

    private String manufacturer;

    private String model;

    private String serialNumber;

    private LocalDate purchaseDate;

    private BigDecimal purchasePrice;

    private BigDecimal currentValue;

    private BigDecimal replacementCost;

    private LocalDate warrantyExpiryDate;

    private LocalDate installationDate;

    private Integer expectedLifespanYears;

    private LocalDate decommissionDate;

    private String tagNumber;

    private List<String> imageUrls;

    private String description;

    private String notes;

    private Object specifications;

    private Boolean isDeleted;

    private String createdBy;

    private Instant createdAt;

    private String updatedBy;

    private Instant updatedAt;
}










