package com.QhomeBase.assetmaintenanceservice.dto.asset;

import com.QhomeBase.assetmaintenanceservice.model.AssetStatus;
import com.QhomeBase.assetmaintenanceservice.model.AssetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAssetRequest {

    @NotNull(message = "Building ID is required")
    private java.util.UUID buildingId;

    private java.util.UUID unitId; // Optional - for unit-level assets

    @NotBlank(message = "Asset code is required")
    private String code;

    @NotBlank(message = "Asset name is required")
    private String name;

    @NotNull(message = "Asset type is required")
    private AssetType assetType;

    @Builder.Default
    private AssetStatus status = AssetStatus.ACTIVE;

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

    private String tagNumber;

    private List<String> imageUrls;

    private String description;

    private String notes;

    private Object specifications;
}










