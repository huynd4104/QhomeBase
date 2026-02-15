package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.imports.AssetImportResponse;
import com.QhomeBase.baseservice.service.exports.AssetExportService;
import com.QhomeBase.baseservice.service.imports.AssetImportService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.UUID;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
@Slf4j
public class AssetImportController {

    private final AssetImportService assetImportService;
    private final AssetExportService assetExportService;

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@authz.canCreateAssets() || @authz.canManageAssets()")
    @Operation(summary = "Import tài sản từ file Excel vào một Tòa nhà")
    public ResponseEntity<?> importAssets(
            @RequestParam("file") MultipartFile file,
            @RequestParam("buildingId") UUID buildingId) {

        log.info("Request import assets for building: {}", buildingId);

        try {
            AssetImportResponse response = assetImportService.importAssets(file, buildingId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("[ImportAsset] Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(buildErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("[ImportAsset] Unexpected error", e);
            return ResponseEntity.internalServerError().body(buildErrorResponse("Lỗi hệ thống: " + e.getMessage()));
        }
    }

    // --- 2. API TẢI TEMPLATE (Mới thêm theo yêu cầu của bạn) ---
    @GetMapping(value = "/import/template", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @PreAuthorize("@authz.canViewAssets()")
    @Operation(summary = "Tải file mẫu Excel để nhập liệu tài sản")
    public ResponseEntity<byte[]> downloadTemplate() {
        try {
            // Gọi Service để tạo file mẫu chuẩn
            byte[] bytes = assetImportService.generateTemplateWorkbook();

            String filename = "asset_import_template.xlsx";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        } catch (Exception e) {
            log.error("Failed to generate asset template", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // --- 3. API EXPORT ---
    @GetMapping(value = "/export")
    @PreAuthorize("@authz.canViewAssets()")
    @Operation(summary = "Export danh sách tài sản hiện có ra Excel")
    public ResponseEntity<byte[]> exportAssetsToExcel(
            @RequestParam(required = false) String buildingId,
            @RequestParam(required = false) String unitId,
            @RequestParam(required = false) String assetType) {

        try {
            byte[] bytes = assetExportService.exportAssetsToExcel(buildingId, unitId, assetType);

            String filename = String.format("danh_sach_thiet_bi_%s.xlsx",
                    java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("ddMMyyyy")));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);

        } catch (Exception e) {
            log.error("Failed to export assets", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private AssetImportResponse buildErrorResponse(String message) {
        return AssetImportResponse.builder()
                .successCount(0)
                .errorCount(0)
                .hasValidationErrors(true)
                .validationErrors(Collections.singletonList(message))
                .build();
    }
}