package com.QhomeBase.baseservice.controller;

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

import java.io.IOException;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
@Slf4j
public class AssetImportController {

    private final AssetImportService assetImportService;
    private final AssetExportService assetExportService;

    // --- 1. API IMPORT (giống Resident: trả error Excel) ---
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@authz.canCreateAssets() || @authz.canManageAssets()")
    @Operation(summary = "Import tài sản từ file Excel")
    public ResponseEntity<byte[]> importAssets(
            @RequestParam("file") MultipartFile file) throws IOException {

        log.info("Request import assets from file: {}", file.getOriginalFilename());

        byte[] errorReport = assetImportService.importAssets(file.getInputStream());

        if (errorReport.length > 0) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "import_errors.xlsx");
            return ResponseEntity.badRequest()
                    .headers(headers)
                    .body(errorReport);
        }

        return ResponseEntity.ok().build();
    }

    // --- 2. API TẢI TEMPLATE ---
    @GetMapping(value = "/import/template", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @PreAuthorize("@authz.canViewAssets()")
    @Operation(summary = "Tải file mẫu Excel để nhập liệu tài sản")
    public ResponseEntity<byte[]> downloadTemplate() {
        try {
            byte[] bytes = assetImportService.generateTemplateWorkbook();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "asset_import_template.xlsx");

            return ResponseEntity.ok()
                    .headers(headers)
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
                    .contentType(MediaType
                            .parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);

        } catch (Exception e) {
            log.error("Failed to export assets", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}