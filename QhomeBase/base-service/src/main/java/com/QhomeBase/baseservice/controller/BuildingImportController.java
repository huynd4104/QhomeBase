package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.imports.BuildingImportResponse;
import com.QhomeBase.baseservice.service.imports.BuildingImportService;
import com.QhomeBase.baseservice.service.imports.BuildingExportService;
import com.QhomeBase.baseservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/buildings")
@RequiredArgsConstructor
@Slf4j
public class BuildingImportController {

    private final BuildingImportService buildingImportService;
    private final BuildingExportService buildingExportService;

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@authz.canCreateBuilding()")
    public ResponseEntity<?> importBuildings(
            @RequestParam("file") MultipartFile file,
            Authentication auth
    ) {
        try {
            String createdBy = "import";
            if (auth != null && auth.getPrincipal() instanceof UserPrincipal up) {
                createdBy = up.username();
            }
            BuildingImportResponse response = buildingImportService.importBuildings(file, createdBy);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("[ImportBuilding] Bad request: {}", e.getMessage());
            // Tạo response với lỗi validation
            BuildingImportResponse errorResponse = BuildingImportResponse.builder()
                    .hasValidationErrors(true)
                    .validationErrors(java.util.List.of(e.getMessage()))
                    .build();
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (IllegalStateException e) {
            log.warn("[ImportBuilding] Illegal state: {}", e.getMessage());
            // Tạo response với lỗi validation
            BuildingImportResponse errorResponse = BuildingImportResponse.builder()
                    .hasValidationErrors(true)
                    .validationErrors(java.util.List.of(e.getMessage()))
                    .build();
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            log.error("[ImportBuilding] Unexpected error", e);
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Đã xảy ra lỗi không xác định khi import tòa nhà";
            // Tạo response với lỗi
            BuildingImportResponse errorResponse = BuildingImportResponse.builder()
                    .hasValidationErrors(true)
                    .validationErrors(java.util.List.of(errorMessage))
                    .build();
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping(value = "/import/template", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @PreAuthorize("@authz.canViewBuildings()")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] bytes = buildingImportService.generateTemplateWorkbook();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"building_import_template.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @GetMapping(value = "/export", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @PreAuthorize("@authz.canViewBuildings()")
    public ResponseEntity<byte[]> exportBuildings(
            @RequestParam(value = "withUnits", defaultValue = "false") boolean withUnits
    ) {
        byte[] bytes;
        String filename;
        if (withUnits) {
            bytes = buildingExportService.exportBuildingsWithUnitsToExcel();
            filename = "buildings_with_units_export.xlsx";
        } else {
            bytes = buildingExportService.exportBuildingsToExcel();
            filename = "buildings_export.xlsx";
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }
}


