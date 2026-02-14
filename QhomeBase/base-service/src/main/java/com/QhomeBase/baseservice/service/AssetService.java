package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.AssetDto;
import com.QhomeBase.baseservice.model.Asset;
import com.QhomeBase.baseservice.model.AssetType;
import com.QhomeBase.baseservice.model.RoomType;
import com.QhomeBase.baseservice.model.Unit;
import com.QhomeBase.baseservice.repository.AssetRepository;
import com.QhomeBase.baseservice.repository.UnitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetRepository assetRepository;
    private final UnitRepository unitRepository;

    @Transactional(readOnly = true)
    public List<AssetDto> getAllAssets() {
        return assetRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AssetDto> getAssetsByUnitId(UUID unitId) {
        return assetRepository.findByUnitId(unitId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AssetDto> getAssetsByBuildingId(UUID buildingId) {
        return assetRepository.findByBuildingId(buildingId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AssetDto> getAssetsByAssetType(AssetType assetType) {
        return assetRepository.findByAssetType(assetType).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AssetDto> getAssetsByUnitIdAndRoomType(UUID unitId, RoomType roomType) {
        return assetRepository.findByUnitIdAndRoomType(unitId, roomType).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<RoomType, List<AssetDto>> getAssetsByUnitIdGroupedByRoom(UUID unitId) {
        return assetRepository.findByUnitIdAndActiveTrue(unitId).stream()
                .map(this::toDto)
                .collect(Collectors.groupingBy(AssetDto::roomType));
    }

    @Transactional(readOnly = true)
    public AssetDto getAssetById(UUID id) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found: " + id));
        return toDto(asset);
    }

    @Transactional
    public AssetDto createAsset(CreateAssetRequest req) {
        Unit unit = unitRepository.findById(req.unitId())
                .orElseThrow(() -> new IllegalArgumentException("Unit not found: " + req.unitId()));

        if (assetRepository.findByAssetCode(req.assetCode()).isPresent()) {
            throw new IllegalArgumentException("Asset code already exists: " + req.assetCode());
        }

        Asset asset = Asset.builder()
                .unit(unit)
                .assetType(req.assetType())
                .roomType(req.roomType())
                .assetCode(req.assetCode())
                .name(req.name())
                .brand(req.brand())
                .model(req.model())
                .serialNumber(req.serialNumber())
                .description(req.description())
                .warrantyUntil(req.warrantyUntil())
                .active(req.active() != null ? req.active() : true)
                .installedAt(req.installedAt())
                .build();

        Asset saved = assetRepository.save(asset);
        log.info("Created asset: {} for unit: {}", saved.getId(), req.unitId());
        return toDto(saved);
    }

    @Transactional
    public AssetDto updateAsset(UUID id, UpdateAssetRequest req) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found: " + id));

        if (req.assetCode() != null && !req.assetCode().equals(asset.getAssetCode())) {
            if (assetRepository.findByAssetCode(req.assetCode()).isPresent()) {
                throw new IllegalArgumentException("Asset code already exists: " + req.assetCode());
            }
            asset.setAssetCode(req.assetCode());
        }

        if (req.name() != null) asset.setName(req.name());
        if (req.brand() != null) asset.setBrand(req.brand());
        if (req.model() != null) asset.setModel(req.model());
        if (req.serialNumber() != null) asset.setSerialNumber(req.serialNumber());
        if (req.description() != null) asset.setDescription(req.description());
        if (req.warrantyUntil() != null) asset.setWarrantyUntil(req.warrantyUntil());
        if (req.active() != null) asset.setActive(req.active());
        if (req.installedAt() != null) asset.setInstalledAt(req.installedAt());

        Asset updated = assetRepository.save(asset);
        log.info("Updated asset: {}", updated.getId());
        return toDto(updated);
    }

    @Transactional
    public void deleteAsset(UUID id) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found: " + id));
        assetRepository.delete(asset);
        log.info("Deleted asset: {}", id);
    }

    @Transactional
    public AssetDto deactivateAsset(UUID id) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found: " + id));
        asset.setActive(false);
        asset.setRemovedAt(LocalDate.now());
        Asset updated = assetRepository.save(asset);
        log.info("Deactivated asset: {}", id);
        return toDto(updated);
    }

    private AssetDto toDto(Asset asset) {
        UUID buildingId = asset.getUnit() != null && asset.getUnit().getBuilding() != null
                ? asset.getUnit().getBuilding().getId()
                : null;
        String buildingCode = asset.getUnit() != null && asset.getUnit().getBuilding() != null
                ? asset.getUnit().getBuilding().getCode()
                : null;

        return new AssetDto(
                asset.getId(),
                asset.getUnit() != null ? asset.getUnit().getId() : null,
                buildingId,
                buildingCode,
                asset.getUnit() != null ? asset.getUnit().getCode() : null,
                asset.getUnit() != null ? asset.getUnit().getFloor() : null,
                asset.getAssetType(),
                asset.getRoomType(),
                asset.getAssetCode(),
                asset.getName(),
                asset.getBrand(),
                asset.getModel(),
                asset.getSerialNumber(),
                asset.getDescription(),
                asset.getActive(),
                asset.getInstalledAt(),
                asset.getRemovedAt(),
                asset.getWarrantyUntil(),
                asset.getCreatedAt(),
                asset.getUpdatedAt());
    }

    public record CreateAssetRequest(
            UUID unitId,
            RoomType roomType,
            AssetType assetType,
            String assetCode,
            String name,
            String brand,
            String model,
            String serialNumber,
            String description,
            Boolean active,
            LocalDate installedAt,
            LocalDate warrantyUntil
    ) {}

    public record UpdateAssetRequest(
            String assetCode,
            String name,
            String brand,
            String model,
            String serialNumber,
            String description,
            Boolean active,
            LocalDate installedAt,
            LocalDate warrantyUntil
    ) {}
}
