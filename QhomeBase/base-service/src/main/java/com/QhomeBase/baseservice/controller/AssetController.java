package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.AssetDto;
import com.QhomeBase.baseservice.model.AssetType;
import com.QhomeBase.baseservice.model.RoomType;
import com.QhomeBase.baseservice.service.AssetService;
import com.QhomeBase.baseservice.service.exports.AssetExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
@Slf4j
public class AssetController {

    private final AssetService assetService;
    private final AssetExportService assetExportService;

    @GetMapping
    public ResponseEntity<List<AssetDto>> getAllAssets() {
        List<AssetDto> result = assetService.getAllAssets();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssetDto> getAssetById(@PathVariable UUID id) {
        AssetDto result = assetService.getAssetById(id);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/unit/{unitId}")
    public ResponseEntity<List<AssetDto>> getAssetsByUnit(
            @PathVariable UUID unitId,
            @RequestParam(required = false) RoomType roomType) {
        List<AssetDto> result;
        if (roomType != null) {
            result = assetService.getAssetsByUnitIdAndRoomType(unitId, roomType);
        } else {
            result = assetService.getAssetsByUnitId(unitId);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/unit/{unitId}/grouped-by-room")
    public ResponseEntity<Map<RoomType, List<AssetDto>>> getAssetsByUnitGroupedByRoom(
            @PathVariable UUID unitId) {
        Map<RoomType, List<AssetDto>> result = assetService.getAssetsByUnitIdGroupedByRoom(unitId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/building/{buildingId}")
    public ResponseEntity<List<AssetDto>> getAssetsByBuilding(@PathVariable UUID buildingId) {
        List<AssetDto> result = assetService.getAssetsByBuildingId(buildingId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/type/{assetType}")
    public ResponseEntity<List<AssetDto>> getAssetsByType(@PathVariable AssetType assetType) {
        List<AssetDto> result = assetService.getAssetsByAssetType(assetType);
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<AssetDto> createAsset(@RequestBody AssetService.CreateAssetRequest req) {
        AssetDto result = assetService.createAsset(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AssetDto> updateAsset(
            @PathVariable UUID id,
            @RequestBody AssetService.UpdateAssetRequest req) {
        AssetDto result = assetService.updateAsset(id, req);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAsset(@PathVariable UUID id) {
        assetService.deleteAsset(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<AssetDto> deactivateAsset(@PathVariable UUID id) {
        AssetDto result = assetService.deactivateAsset(id);
        return ResponseEntity.ok(result);
    }
}
