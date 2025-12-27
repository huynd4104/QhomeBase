package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.AssetDto;
import com.QhomeBase.baseservice.model.AssetType;
import com.QhomeBase.baseservice.service.AssetService;
import com.QhomeBase.baseservice.service.exports.AssetExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    public ResponseEntity<List<AssetDto>> getAssetsByUnit(@PathVariable UUID unitId) {
        List<AssetDto> result = assetService.getAssetsByUnitId(unitId);
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

    @GetMapping(value = "/export", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportAssetsToExcel(
            @RequestParam(required = false) String buildingId,
            @RequestParam(required = false) String unitId,
            @RequestParam(required = false) String assetType) {
        try {
            byte[] bytes = assetExportService.exportAssetsToExcel(buildingId, unitId, assetType);
            
            String filename = String.format("danh_sach_thiet_bi_%s.xlsx", 
                    java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")));
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(bytes);
        } catch (Exception e) {
            log.error("Failed to export assets", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}


