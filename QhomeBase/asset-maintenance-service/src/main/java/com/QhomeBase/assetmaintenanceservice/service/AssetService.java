package com.QhomeBase.assetmaintenanceservice.service;

import com.QhomeBase.assetmaintenanceservice.client.BaseServiceClient;
import com.QhomeBase.assetmaintenanceservice.dto.asset.AssetResponse;
import com.QhomeBase.assetmaintenanceservice.dto.asset.CreateAssetRequest;
import com.QhomeBase.assetmaintenanceservice.dto.asset.UpdateAssetRequest;
import com.QhomeBase.assetmaintenanceservice.model.Asset;
import com.QhomeBase.assetmaintenanceservice.model.AssetStatus;
import com.QhomeBase.assetmaintenanceservice.model.AssetType;
import com.QhomeBase.assetmaintenanceservice.repository.AssetRepository;
import com.QhomeBase.assetmaintenanceservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class AssetService {
    private final AssetRepository assetRepository;
    private final BaseServiceClient baseServiceClient;

    public AssetResponse getAssetById(UUID id) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found with ID: " + id));
        if (asset.getIsDeleted()) {
            throw new IllegalArgumentException("Asset has been deleted");
        }
        return toDto(asset);
    }

    public Page<AssetResponse> getAllAssets(UUID buildingId, UUID unitId, AssetType assetType, AssetStatus status, Boolean isDeleted, Pageable pageable) {
        if (isDeleted != null && isDeleted) {
            return assetRepository.findAll(pageable).map(this::toDto);
        }
        
        Page<Asset> assets = assetRepository.findWithFilters(buildingId, unitId, assetType, status, pageable);
        return assets.map(this::toDto);
    }

    public List<AssetResponse> getAssetsByBuildingId(UUID buildingId) {
        List<Asset> assets = assetRepository.findByBuildingIdAndIsDeletedFalse(buildingId);
        return assets.stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<AssetResponse> getAssetsByUnitId(UUID unitId) {
        List<Asset> assets = assetRepository.findByUnitIdAndIsDeletedFalse(unitId);
        return assets.stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<AssetResponse> searchAssets(String query, UUID buildingId, int limit) {
        Pageable pageable = Pageable.ofSize(limit);
        List<Asset> assets = assetRepository.searchAssets(query, buildingId, pageable);
        return assets.stream().map(this::toDto).collect(Collectors.toList());
    }

    public AssetResponse create(CreateAssetRequest request, Authentication authentication) {
        validateAssetRequest(request);
        
       var p = (UserPrincipal) authentication.getPrincipal();
       UUID id =  p.uid();
        
        Asset asset = Asset.builder()
                .buildingId(request.getBuildingId())
                .unitId(request.getUnitId())
                .code(request.getCode())
                .name(request.getName())
                .assetType(request.getAssetType())
                .status(request.getStatus() != null ? request.getStatus() : AssetStatus.ACTIVE)
                .location(request.getLocation())
                .manufacturer(request.getManufacturer())
                .model(request.getModel())
                .serialNumber(request.getSerialNumber())
                .purchaseDate(request.getPurchaseDate())
                .purchasePrice(request.getPurchasePrice())
                .currentValue(request.getCurrentValue())
                .replacementCost(request.getReplacementCost())
                .warrantyExpiryDate(request.getWarrantyExpiryDate())
                .installationDate(request.getInstallationDate())
                .expectedLifespanYears(request.getExpectedLifespanYears())
                .tagNumber(request.getTagNumber())
                .imageUrls(request.getImageUrls())
                .description(request.getDescription())
                .notes(request.getNotes())
                .specifications(request.getSpecifications())
                .isDeleted(false)
                .createdBy(id.toString())
                .createdAt(Instant.now())
                .updatedBy(id.toString())
                .updatedAt(Instant.now())
                .build();

        Asset savedAsset = assetRepository.save(asset);
        return toDto(savedAsset);
    }

    public AssetResponse update(UUID assetId, UpdateAssetRequest request, Authentication authentication) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found with ID: " + assetId));
        
        if (asset.getIsDeleted()) {
            throw new IllegalArgumentException("Cannot update deleted asset");
        }

        var p = (UserPrincipal) authentication.getPrincipal();
        UUID userId = p.uid();

        validateUpdateRequest(asset, request);

        if (request.getBuildingId() != null) {
            asset.setBuildingId(request.getBuildingId());
        }
        if (request.getUnitId() != null) {
            asset.setUnitId(request.getUnitId());
        }
        if (request.getCode() != null) {
            asset.setCode(request.getCode());
        }
        if (request.getName() != null) {
            asset.setName(request.getName());
        }
        if (request.getAssetType() != null) {
            asset.setAssetType(request.getAssetType());
        }
        if (request.getStatus() != null) {
            asset.setStatus(request.getStatus());
        }
        if (request.getLocation() != null) {
            asset.setLocation(request.getLocation());
        }
        if (request.getManufacturer() != null) {
            asset.setManufacturer(request.getManufacturer());
        }
        if (request.getModel() != null) {
            asset.setModel(request.getModel());
        }
        if (request.getSerialNumber() != null) {
            asset.setSerialNumber(request.getSerialNumber());
        }
        if (request.getPurchaseDate() != null) {
            asset.setPurchaseDate(request.getPurchaseDate());
        }
        if (request.getPurchasePrice() != null) {
            asset.setPurchasePrice(request.getPurchasePrice());
        }
        if (request.getCurrentValue() != null) {
            asset.setCurrentValue(request.getCurrentValue());
        }
        if (request.getReplacementCost() != null) {
            asset.setReplacementCost(request.getReplacementCost());
        }
        if (request.getWarrantyExpiryDate() != null) {
            asset.setWarrantyExpiryDate(request.getWarrantyExpiryDate());
        }
        if (request.getInstallationDate() != null) {
            asset.setInstallationDate(request.getInstallationDate());
        }
        if (request.getExpectedLifespanYears() != null) {
            asset.setExpectedLifespanYears(request.getExpectedLifespanYears());
        }
        if (request.getDecommissionDate() != null) {
            asset.setDecommissionDate(request.getDecommissionDate());
        }
        if (request.getTagNumber() != null) {
            asset.setTagNumber(request.getTagNumber());
        }
        if (request.getImageUrls() != null) {
            asset.setImageUrls(request.getImageUrls());
        }
        if (request.getDescription() != null) {
            asset.setDescription(request.getDescription());
        }
        if (request.getNotes() != null) {
            asset.setNotes(request.getNotes());
        }
        if (request.getSpecifications() != null) {
            asset.setSpecifications(request.getSpecifications());
        }

        asset.setUpdatedBy(userId.toString());
        asset.setUpdatedAt(Instant.now());

        Asset updatedAsset = assetRepository.save(asset);
        return toDto(updatedAsset);
    }

    public void delete(UUID assetId, Authentication authentication) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found with ID: " + assetId));
        
        if (asset.getIsDeleted()) {
            throw new IllegalArgumentException("Asset already deleted");
        }

        var p = (UserPrincipal) authentication.getPrincipal();
        UUID userId = p.uid();

        asset.setIsDeleted(true);
        asset.setUpdatedBy(userId.toString());
        asset.setUpdatedAt(Instant.now());
        assetRepository.save(asset);
    }

    public AssetResponse restore(UUID assetId, Authentication authentication) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found with ID: " + assetId));
        
        if (!asset.getIsDeleted()) {
            throw new IllegalArgumentException("Asset is not deleted");
        }

        var p = (UserPrincipal) authentication.getPrincipal();
        UUID userId = p.uid();

        asset.setIsDeleted(false);
        asset.setUpdatedBy(userId.toString());
        asset.setUpdatedAt(Instant.now());
        
        Asset restoredAsset = assetRepository.save(asset);
        return toDto(restoredAsset);
    }

    private void validateAssetRequest(CreateAssetRequest request) {
        if (!baseServiceClient.buildingExists(request.getBuildingId())) {
            throw new IllegalArgumentException("Building not found: " + request.getBuildingId());
        }

        if (request.getUnitId() != null) {
            if (!baseServiceClient.unitExists(request.getUnitId())) {
                throw new IllegalArgumentException("Unit not found: " + request.getUnitId());
            }
            if (!baseServiceClient.unitBelongsToBuilding(request.getUnitId(), request.getBuildingId())) {
                throw new IllegalArgumentException("Unit does not belong to building");
            }
        }

        if (request.getUnitId() == null) {
            if (assetRepository.existsByCodeAndBuildingId(request.getCode(), request.getBuildingId())) {
                throw new IllegalArgumentException("Asset code already exists for building: " + request.getCode());
            }
        } else {
            if (assetRepository.existsByCodeAndUnitId(request.getCode(), request.getUnitId())) {
                throw new IllegalArgumentException("Asset code already exists for unit: " + request.getCode());
            }
        }

        LocalDate now = LocalDate.now();
        
        if (request.getPurchaseDate() != null && request.getPurchaseDate().isAfter(now)) {
            throw new IllegalArgumentException("Purchase date cannot be in the future");
        }

        if (request.getInstallationDate() != null && request.getPurchaseDate() != null) {
            if (request.getInstallationDate().isBefore(request.getPurchaseDate())) {
                throw new IllegalArgumentException("Installation date cannot be before purchase date");
            }
        }

        if (request.getWarrantyExpiryDate() != null && request.getPurchaseDate() != null) {
            if (request.getWarrantyExpiryDate().isBefore(request.getPurchaseDate())) {
                throw new IllegalArgumentException("Warranty expiry date cannot be before purchase date");
            }
        }
    }

    private void validateUpdateRequest(Asset existingAsset, UpdateAssetRequest request) {
        if (request.getBuildingId() != null && !baseServiceClient.buildingExists(request.getBuildingId())) {
            throw new IllegalArgumentException("Building not found: " + request.getBuildingId());
        }

        if (request.getUnitId() != null) {
            if (!baseServiceClient.unitExists(request.getUnitId())) {
                throw new IllegalArgumentException("Unit not found: " + request.getUnitId());
            }
            UUID buildingId = request.getBuildingId() != null ? request.getBuildingId() : existingAsset.getBuildingId();
            if (!baseServiceClient.unitBelongsToBuilding(request.getUnitId(), buildingId)) {
                throw new IllegalArgumentException("Unit does not belong to building");
            }
        }

        String newCode = request.getCode();
        if (newCode != null && !newCode.equals(existingAsset.getCode())) {
            UUID buildingId = request.getBuildingId() != null ? request.getBuildingId() : existingAsset.getBuildingId();
            UUID unitId = request.getUnitId() != null ? request.getUnitId() : existingAsset.getUnitId();
            
            if (unitId == null) {
                if (assetRepository.existsByCodeAndBuildingId(newCode, buildingId)) {
                    throw new IllegalArgumentException("Asset code already exists for building: " + newCode);
                }
            } else {
                if (assetRepository.existsByCodeAndUnitId(newCode, unitId)) {
                    throw new IllegalArgumentException("Asset code already exists for unit: " + newCode);
                }
            }
        }

        LocalDate now = LocalDate.now();
        
        if (request.getPurchaseDate() != null && request.getPurchaseDate().isAfter(now)) {
            throw new IllegalArgumentException("Purchase date cannot be in the future");
        }

        LocalDate purchaseDate = request.getPurchaseDate() != null ? request.getPurchaseDate() : existingAsset.getPurchaseDate();
        
        if (request.getInstallationDate() != null && purchaseDate != null) {
            if (request.getInstallationDate().isBefore(purchaseDate)) {
                throw new IllegalArgumentException("Installation date cannot be before purchase date");
            }
        }

        if (request.getWarrantyExpiryDate() != null && purchaseDate != null) {
            if (request.getWarrantyExpiryDate().isBefore(purchaseDate)) {
                throw new IllegalArgumentException("Warranty expiry date cannot be before purchase date");
            }
        }
    }


    public AssetResponse toDto(Asset asset) {
        if (asset == null) {
            return null;
        }

        return AssetResponse.builder()
                .id(asset.getId())
                .buildingId(asset.getBuildingId())
                .unitId(asset.getUnitId())
                .code(asset.getCode())
                .name(asset.getName())
                .assetType(asset.getAssetType())
                .status(asset.getStatus())
                .location(asset.getLocation())
                .manufacturer(asset.getManufacturer())
                .model(asset.getModel())
                .serialNumber(asset.getSerialNumber())
                .purchaseDate(asset.getPurchaseDate())
                .purchasePrice(asset.getPurchasePrice())
                .currentValue(asset.getCurrentValue())
                .replacementCost(asset.getReplacementCost())
                .warrantyExpiryDate(asset.getWarrantyExpiryDate())
                .installationDate(asset.getInstallationDate())
                .expectedLifespanYears(asset.getExpectedLifespanYears())
                .decommissionDate(asset.getDecommissionDate())
                .tagNumber(asset.getTagNumber())
                .imageUrls(asset.getImageUrls())
                .description(asset.getDescription())
                .notes(asset.getNotes())
                .specifications(asset.getSpecifications())
                .isDeleted(asset.getIsDeleted())
                .createdBy(asset.getCreatedBy())
                .createdAt(asset.getCreatedAt())
                .updatedBy(asset.getUpdatedBy())
                .updatedAt(asset.getUpdatedAt())
                .build();
    }
}
