package com.QhomeBase.assetmaintenanceservice.controller;

import com.QhomeBase.assetmaintenanceservice.dto.service.CreateServiceRequest;
import com.QhomeBase.assetmaintenanceservice.dto.service.ServiceAvailabilityDto;
import com.QhomeBase.assetmaintenanceservice.dto.service.ServiceAvailabilityRequest;
import com.QhomeBase.assetmaintenanceservice.dto.service.ServiceDto;
import com.QhomeBase.assetmaintenanceservice.dto.service.UpdateServiceRequest;
import com.QhomeBase.assetmaintenanceservice.service.ServiceConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/asset-maintenance/services")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceConfigService serviceConfigService;

    @GetMapping
    @PreAuthorize("@authz.canViewServiceConfig()")
    public ResponseEntity<List<ServiceDto>> getServices(@RequestParam(required = false) Boolean isActive) {
        return ResponseEntity.ok(serviceConfigService.findAll(isActive));
    }

    @PostMapping
    @PreAuthorize("@authz.canManageServiceConfig()")
    public ResponseEntity<ServiceDto> createService(@Valid @RequestBody CreateServiceRequest request) {
        ServiceDto created = serviceConfigService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.canViewServiceConfig()")
    public ResponseEntity<ServiceDto> getServiceById(@PathVariable UUID id) {
        ServiceDto service = serviceConfigService.findById(id);
        return ResponseEntity.ok(service);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.canManageServiceConfig()")
    public ResponseEntity<ServiceDto> updateService(@PathVariable UUID id,
                                                    @Valid @RequestBody UpdateServiceRequest request) {
        ServiceDto updated = serviceConfigService.update(id, request);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@authz.canManageServiceConfig()")
    public ResponseEntity<ServiceDto> toggleStatus(@PathVariable UUID id,
                                                   @RequestParam("active") boolean active) {
        ServiceDto updated = serviceConfigService.setActive(id, active);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}/availabilities")
    @PreAuthorize("@authz.canViewServiceConfig()")
    public ResponseEntity<List<ServiceAvailabilityDto>> getServiceAvailabilities(@PathVariable UUID id) {
        return ResponseEntity.ok(serviceConfigService.findAvailability(id));
    }

    @PostMapping("/{id}/availabilities")
    @PreAuthorize("@authz.canManageServiceConfig()")
    public ResponseEntity<List<ServiceAvailabilityDto>> addAvailability(@PathVariable UUID id,
                                                                        @Valid @RequestBody ServiceAvailabilityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceConfigService.addAvailability(id, request));
    }

    @PutMapping("/{id}/availabilities/{availabilityId}")
    @PreAuthorize("@authz.canManageServiceConfig()")
    public ResponseEntity<ServiceAvailabilityDto> updateAvailability(@PathVariable UUID id,
                                                                     @PathVariable UUID availabilityId,
                                                                     @Valid @RequestBody ServiceAvailabilityRequest request) {
        return ResponseEntity.ok(serviceConfigService.updateAvailability(id, availabilityId, request));
    }

    @DeleteMapping("/{id}/availabilities/{availabilityId}")
    @PreAuthorize("@authz.canManageServiceConfig()")
    public ResponseEntity<List<ServiceAvailabilityDto>> deleteAvailability(@PathVariable UUID id,
                                                                           @PathVariable UUID availabilityId) {
        return ResponseEntity.ok(serviceConfigService.deleteAvailability(id, availabilityId));
    }

    @GetMapping("/public")
    public ResponseEntity<List<ServiceDto>> getPublicServices(@RequestParam(required = false) Boolean isActive) {
        return ResponseEntity.ok(serviceConfigService.findAll(isActive));
    }
}

