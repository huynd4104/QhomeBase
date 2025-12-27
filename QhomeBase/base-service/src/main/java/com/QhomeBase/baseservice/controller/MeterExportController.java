package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.service.imports.MeterExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/meters/export")
@PreAuthorize("@authz.canViewUnits()")
@RequiredArgsConstructor
@Slf4j
public class MeterExportController {

    private final MeterExportService meterExportService;

    @GetMapping
    public ResponseEntity<byte[]> exportMeters(@RequestParam(required = false) UUID buildingId) {
        log.info("Exporting meters for buildingId={}", buildingId);
        try {
            byte[] bytes = meterExportService.exportMeters(buildingId);
            log.info("Exported {} bytes for buildingId={}", bytes.length, buildingId);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"meters_export.xlsx\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        } catch (Exception e) {
            log.error("Failed to export meters for buildingId={}", buildingId, e);
            return ResponseEntity.status(500).build();
        }
    }
}

