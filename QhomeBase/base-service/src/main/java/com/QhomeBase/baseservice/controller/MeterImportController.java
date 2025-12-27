package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.imports.MeterImportResponse;
import com.QhomeBase.baseservice.service.imports.MeterImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/meters/import")
@PreAuthorize("@authz.canViewUnits()")
@RequiredArgsConstructor
@Slf4j
public class MeterImportController {

    private final MeterImportService meterImportService;

    @PostMapping
    public ResponseEntity<MeterImportResponse> importMeters(@RequestParam("file") MultipartFile file) {
        try {
            MeterImportResponse response = meterImportService.importMeters(file);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("[MeterImport] Bad request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("[MeterImport] Error importing meters", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] bytes = meterImportService.generateTemplateWorkbook();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"meter_import_template.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }
}

