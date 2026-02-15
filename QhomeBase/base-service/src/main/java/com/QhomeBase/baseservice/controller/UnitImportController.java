package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.imports.UnitImportResponse;
import com.QhomeBase.baseservice.service.imports.UnitImportService;
import com.QhomeBase.baseservice.service.exports.UnitExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/units")
@RequiredArgsConstructor
@Slf4j
public class UnitImportController {

    private final UnitImportService unitImportService;
    private final UnitExportService unitExportService;

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@authz.canCreateUnits() || @authz.canViewUnits()")
    public ResponseEntity<?> importUnits(@RequestParam("file") MultipartFile file) {
        try {
            UnitImportResponse response = unitImportService.importUnits(file);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("[ImportUnit] Bad request: {}", e.getMessage());
            // Tạo response với lỗi validation
            UnitImportResponse errorResponse = UnitImportResponse.builder()
                    .hasValidationErrors(true)
                    .validationErrors(java.util.List.of(e.getMessage()))
                    .build();
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (IllegalStateException e) {
            log.warn("[ImportUnit] Illegal state: {}", e.getMessage());
            // Tạo response với lỗi validation
            UnitImportResponse errorResponse = UnitImportResponse.builder()
                    .hasValidationErrors(true)
                    .validationErrors(java.util.List.of(e.getMessage()))
                    .build();
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            log.error("[ImportUnit] Unexpected error", e);
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Đã xảy ra lỗi không xác định khi import căn hộ";
            // Tạo response với lỗi
            UnitImportResponse errorResponse = UnitImportResponse.builder()
                    .hasValidationErrors(true)
                    .validationErrors(java.util.List.of(errorMessage))
                    .build();
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping(value = "/import/template", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @PreAuthorize("@authz.canViewUnits()")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] bytes = unitImportService.generateTemplateWorkbook();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"unit_import_template.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @GetMapping(value = "/export", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @PreAuthorize("@authz.canViewUnits()")
    public ResponseEntity<byte[]> exportUnits(
            @RequestParam(value = "buildingId", required = false) UUID buildingId,
            @RequestParam(value = "floor", required = false) Integer floor) {
        log.info("Export request - buildingId: {}, floor: {}", buildingId, floor);
        byte[] bytes;
        if (buildingId != null && floor != null) {
            log.info("Exporting by building and floor");
            bytes = unitExportService.exportUnitsByBuildingAndFloorToExcel(buildingId, floor);
        } else if (buildingId != null) {
            log.info("Exporting by building only");
            bytes = unitExportService.exportUnitsByBuildingToExcel(buildingId);
        } else {
            log.info("Exporting all units");
            bytes = unitExportService.exportUnitsToExcel();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"units_export.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }
}


