package com.QhomeBase.assetmaintenanceservice.controller;

import com.QhomeBase.assetmaintenanceservice.dto.maintenance.BulkCreateScheduleResponse;
import com.QhomeBase.assetmaintenanceservice.dto.maintenance.CreateMaintenanceScheduleRequest;
import com.QhomeBase.assetmaintenanceservice.dto.maintenance.MaintenanceScheduleResponse;
import com.QhomeBase.assetmaintenanceservice.dto.maintenance.UpdateMaintenanceScheduleRequest;
import com.QhomeBase.assetmaintenanceservice.model.AssetType;
import com.QhomeBase.assetmaintenanceservice.service.MaintenanceScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/asset-maintenance/maintenance/schedules")
@RequiredArgsConstructor
public class AssetScheduleController {

    private final MaintenanceScheduleService maintenanceScheduleService;

    @GetMapping
    @PreAuthorize("@authz.canViewAsset()")
    public ResponseEntity<List<MaintenanceScheduleResponse>> getSchedules(
            @RequestParam(required = false) UUID assetId,
            @RequestParam(required = false) UUID assignedTo,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String maintenanceType
    ) {
        List<MaintenanceScheduleResponse> schedules = maintenanceScheduleService.getAllSchedules(
                assetId,
                assignedTo,
                isActive,
                maintenanceType
        );
        return ResponseEntity.ok(schedules);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.canViewAsset()")
    public ResponseEntity<MaintenanceScheduleResponse> getScheduleById(@PathVariable UUID id) {
        MaintenanceScheduleResponse schedule = maintenanceScheduleService.getScheduleById(id);
        return ResponseEntity.ok(schedule);
    }

    @PostMapping
    @PreAuthorize("@authz.canCreateMaintenanceSchedule()")
    public ResponseEntity<MaintenanceScheduleResponse> createSchedule(
            @Valid @RequestBody CreateMaintenanceScheduleRequest request,
            Authentication authentication
    ) {
        MaintenanceScheduleResponse schedule = maintenanceScheduleService.create(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(schedule);
    }

    @PostMapping("/bulk")
    @PreAuthorize("@authz.canCreateMaintenanceSchedule()")
    public ResponseEntity<BulkCreateScheduleResponse> bulkCreateSchedules(
            @Valid @RequestBody CreateMaintenanceScheduleRequest request,
            @RequestParam AssetType assetType,
            Authentication authentication
    ) {
        BulkCreateScheduleResponse response = maintenanceScheduleService.bulkCreate(request, assetType, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.canUpdateMaintenanceSchedule()")
    public ResponseEntity<MaintenanceScheduleResponse> updateSchedule(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMaintenanceScheduleRequest request,
            Authentication authentication
    ) {
        MaintenanceScheduleResponse schedule = maintenanceScheduleService.update(id, request, authentication);
        return ResponseEntity.ok(schedule);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.canDeleteMaintenanceSchedule()")
    public ResponseEntity<Void> deleteSchedule(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        maintenanceScheduleService.delete(id, authentication);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/toggle")
    @PreAuthorize("@authz.canUpdateMaintenanceSchedule()")
    public ResponseEntity<MaintenanceScheduleResponse> toggleActive(
            @PathVariable UUID id,
            @RequestParam(required = false) Boolean isActive,
            Authentication authentication
    ) {
        MaintenanceScheduleResponse schedule = maintenanceScheduleService.toggleActive(id, isActive, authentication);
        return ResponseEntity.ok(schedule);
    }

    @GetMapping("/by-asset/{assetId}")
    @PreAuthorize("@authz.canViewAsset()")
    public ResponseEntity<List<MaintenanceScheduleResponse>> getSchedulesByAsset(@PathVariable UUID assetId) {
        List<MaintenanceScheduleResponse> schedules = maintenanceScheduleService.getSchedulesByAsset(assetId);
        return ResponseEntity.ok(schedules);
    }

    @GetMapping("/upcoming")
    @PreAuthorize("@authz.canViewAsset()")
    public ResponseEntity<List<MaintenanceScheduleResponse>> getUpcomingSchedules(
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) UUID assetId,
            @RequestParam(required = false) UUID assignedTo
    ) {
        List<MaintenanceScheduleResponse> schedules = maintenanceScheduleService.getUpcomingSchedules(days, assetId, assignedTo);
        return ResponseEntity.ok(schedules);
    }
}
