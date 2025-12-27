package com.QhomeBase.assetmaintenanceservice.controller;

import com.QhomeBase.assetmaintenanceservice.dto.service.CreateServiceComboRequest;
import com.QhomeBase.assetmaintenanceservice.dto.service.ServiceComboDto;
import com.QhomeBase.assetmaintenanceservice.dto.service.UpdateServiceComboItemsRequest;
import com.QhomeBase.assetmaintenanceservice.dto.service.UpdateServiceComboRequest;
import com.QhomeBase.assetmaintenanceservice.service.ServiceComboService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/asset-maintenance")
@RequiredArgsConstructor
public class ServiceComboController {

    private final ServiceComboService serviceComboService;

    @GetMapping("/service-combos")
    @PreAuthorize("@authz.canViewServiceConfig()")
    public ResponseEntity<List<ServiceComboDto>> getAllCombos(@RequestParam(required = false) Boolean isActive) {
        return ResponseEntity.ok(serviceComboService.getAllCombos(isActive));
    }

    @GetMapping("/services/{serviceId}/combos")
    @PreAuthorize("@authz.canViewServiceConfig()")
    public ResponseEntity<List<ServiceComboDto>> getCombos(@PathVariable UUID serviceId,
                                                           @RequestParam(required = false) Boolean isActive) {
        return ResponseEntity.ok(serviceComboService.getCombos(serviceId, isActive));
    }

    @GetMapping("/service-combos/{comboId}")
    @PreAuthorize("@authz.canViewServiceConfig()")
    public ResponseEntity<ServiceComboDto> getComboById(@PathVariable UUID comboId) {
        return ResponseEntity.ok(serviceComboService.getCombo(comboId));
    }

    @PostMapping("/services/{serviceId}/combos")
    @PreAuthorize("@authz.canManageServiceConfig()")
    public ResponseEntity<ServiceComboDto> createCombo(@PathVariable UUID serviceId,
                                                       @Valid @RequestBody CreateServiceComboRequest request) {
        ServiceComboDto created = serviceComboService.createCombo(serviceId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/service-combos/{comboId}")
    @PreAuthorize("@authz.canManageServiceConfig()")
    public ResponseEntity<ServiceComboDto> updateCombo(@PathVariable UUID comboId,
                                                       @Valid @RequestBody UpdateServiceComboRequest request) {
        return ResponseEntity.ok(serviceComboService.updateCombo(comboId, request));
    }

    @PutMapping("/service-combos/{comboId}/items")
    @PreAuthorize("@authz.canManageServiceConfig()")
    public ResponseEntity<ServiceComboDto> updateComboItems(@PathVariable UUID comboId,
                                                            @Valid @RequestBody UpdateServiceComboItemsRequest request) {
        return ResponseEntity.ok(serviceComboService.updateComboItems(comboId, request));
    }

    @PatchMapping("/service-combos/{comboId}/status")
    @PreAuthorize("@authz.canManageServiceConfig()")
    public ResponseEntity<ServiceComboDto> toggleComboStatus(@PathVariable UUID comboId,
                                                             @RequestParam("active") boolean active) {
        return ResponseEntity.ok(serviceComboService.setComboStatus(comboId, active));
    }

    @DeleteMapping("/service-combos/{comboId}")
    @PreAuthorize("@authz.canManageServiceConfig()")
    public ResponseEntity<Void> deleteCombo(@PathVariable UUID comboId) {
        serviceComboService.deleteCombo(comboId);
        return ResponseEntity.noContent().build();
    }
}

