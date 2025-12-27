package com.QhomeBase.assetmaintenanceservice.controller;

import com.QhomeBase.assetmaintenanceservice.dto.asset.AssetResponse;
import com.QhomeBase.assetmaintenanceservice.dto.asset.CreateAssetRequest;
import com.QhomeBase.assetmaintenanceservice.dto.asset.UpdateAssetRequest;
import com.QhomeBase.assetmaintenanceservice.model.AssetStatus;
import com.QhomeBase.assetmaintenanceservice.model.AssetType;
import com.QhomeBase.assetmaintenanceservice.service.AssetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/asset-maintenance/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    @GetMapping
    @PreAuthorize("@authz.canViewAsset()")
    public ResponseEntity<Page<AssetResponse>> getAssets(
            @RequestParam(required = false) UUID buildingId,
            @RequestParam(required = false) UUID unitId,
            @RequestParam(required = false) AssetType assetType,
            @RequestParam(required = false) AssetStatus status,
            @RequestParam(required = false, defaultValue = "false") Boolean isDeleted,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false, defaultValue = "createdAt,desc") String sort
    ) {
        Pageable pageable = PageRequest.of(page, size, resolveSort(sort));
        Page<AssetResponse> assets = assetService.getAllAssets(buildingId, unitId, assetType, status, isDeleted, pageable);
        return ResponseEntity.ok(assets);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.canViewAsset()")
    public ResponseEntity<AssetResponse> getAssetById(@PathVariable UUID id) {
        AssetResponse asset = assetService.getAssetById(id);
        return ResponseEntity.ok(asset);
    }

    @PostMapping
    @PreAuthorize("@authz.canCreateAsset()")
    public ResponseEntity<AssetResponse> createAsset(
            @Valid @RequestBody CreateAssetRequest request,
            Authentication authentication
    ) {
        AssetResponse asset = assetService.create(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(asset);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.canUpdateAsset()")
    public ResponseEntity<AssetResponse> updateAsset(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAssetRequest request,
            Authentication authentication
    ) {
        AssetResponse asset = assetService.update(id, request, authentication);
        return ResponseEntity.ok(asset);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.canDeleteAsset()")
    public ResponseEntity<Void> deleteAsset(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        assetService.delete(id, authentication);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/restore")
    @PreAuthorize("@authz.canUpdateAsset()")
    public ResponseEntity<AssetResponse> restoreAsset(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        AssetResponse asset = assetService.restore(id, authentication);
        return ResponseEntity.ok(asset);
    }

    @GetMapping("/by-building/{buildingId}")
    @PreAuthorize("@authz.canViewAsset()")
    public ResponseEntity<List<AssetResponse>> getAssetsByBuilding(@PathVariable UUID buildingId) {
        List<AssetResponse> assets = assetService.getAssetsByBuildingId(buildingId);
        return ResponseEntity.ok(assets);
    }

    @GetMapping("/by-unit/{unitId}")
    @PreAuthorize("@authz.canViewAsset()")
    public ResponseEntity<List<AssetResponse>> getAssetsByUnit(@PathVariable UUID unitId) {
        List<AssetResponse> assets = assetService.getAssetsByUnitId(unitId);
        return ResponseEntity.ok(assets);
    }

    @GetMapping("/search")
    @PreAuthorize("@authz.canViewAsset()")
    public ResponseEntity<List<AssetResponse>> searchAssets(
            @RequestParam("q") String keyword,
            @RequestParam(required = false) UUID buildingId,
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<AssetResponse> assets = assetService.searchAssets(keyword, buildingId, limit);
        return ResponseEntity.ok(assets);
    }

    private Sort resolveSort(String sort) {
        if (!StringUtils.hasText(sort)) {
            return Sort.by(Sort.Order.desc("createdAt"));
        }

        String[] rawOrders = sort.split(";");
        if (rawOrders.length == 0) {
            return Sort.by(Sort.Order.desc("createdAt"));
        }

        List<Sort.Order> orders = new java.util.ArrayList<>();
        for (String rawOrder : rawOrders) {
            if (!StringUtils.hasText(rawOrder)) {
                continue;
            }
            String[] parts = rawOrder.split(",");
            String property = parts.length > 0 && StringUtils.hasText(parts[0]) ? parts[0].trim() : "createdAt";
            Sort.Direction direction = Sort.Direction.DESC;
            if (parts.length > 1 && StringUtils.hasText(parts[1])) {
                try {
                    direction = Sort.Direction.fromString(parts[1].trim());
                } catch (IllegalArgumentException ignored) {
                    // keep default DESC when direction is invalid
                }
            }
            orders.add(new Sort.Order(direction, property));
        }

        if (orders.isEmpty()) {
            orders.add(new Sort.Order(Sort.Direction.DESC, "createdAt"));
        }

        return Sort.by(orders);
    }
}
