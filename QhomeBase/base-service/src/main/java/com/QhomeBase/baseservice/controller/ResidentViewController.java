package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.residentview.*;
import com.QhomeBase.baseservice.service.ResidentViewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/resident-view")
@RequiredArgsConstructor
@Slf4j
public class ResidentViewController {

    private final ResidentViewService residentViewService;

    @GetMapping("/years")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORTER')")
    public ResponseEntity<List<ResidentViewYearDto>> getYears() {
        return ResponseEntity.ok(residentViewService.getYears());
    }

    @GetMapping("/{year}/buildings")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORTER')")
    public ResponseEntity<List<ResidentViewBuildingDto>> getBuildingsByYear(@PathVariable Integer year) {
        return ResponseEntity.ok(residentViewService.getBuildingsByYear(year));
    }

    @GetMapping("/{year}/buildings/{buildingId}/floors")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORTER')")
    public ResponseEntity<List<ResidentViewFloorDto>> getFloorsByYearAndBuilding(
            @PathVariable Integer year,
            @PathVariable UUID buildingId) {
        return ResponseEntity.ok(residentViewService.getFloorsByYearAndBuilding(year, buildingId));
    }

    @GetMapping("/{year}/buildings/{buildingId}/floors/{floor}/units")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORTER')")
    public ResponseEntity<List<ResidentViewUnitDto>> getUnitsByYearBuildingAndFloor(
            @PathVariable Integer year,
            @PathVariable UUID buildingId,
            @PathVariable Integer floor) {
        return ResponseEntity.ok(residentViewService.getUnitsByYearBuildingAndFloor(year, buildingId, floor));
    }

    @GetMapping("/{year}/units/{unitId}/residents")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORTER')")
    public ResponseEntity<List<ResidentViewResidentDto>> getResidentsByUnitAndYear(
            @PathVariable Integer year,
            @PathVariable UUID unitId) {
        return ResponseEntity.ok(residentViewService.getResidentsByUnitAndYear(unitId, year));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORTER')")
    public ResponseEntity<byte[]> exportResidents(
            @RequestParam Integer year,
            @RequestParam(required = false) UUID buildingId,
            @RequestParam(required = false) Integer floor) {
        byte[] content = residentViewService.exportResidents(year, buildingId, floor);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "residents_" + year + ".xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .body(content);
    }

    @GetMapping("/template")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORTER')")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] content = residentViewService.downloadTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "resident_import_template.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .body(content);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORTER')")
    public ResponseEntity<byte[]> importResidents(@RequestParam("file") MultipartFile file) throws IOException {
        byte[] errorReport = residentViewService.importResidents(file.getInputStream());

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
}
