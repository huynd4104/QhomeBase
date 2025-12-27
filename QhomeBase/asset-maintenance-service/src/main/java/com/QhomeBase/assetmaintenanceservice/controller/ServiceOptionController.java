package com.QhomeBase.assetmaintenanceservice.controller;

import com.QhomeBase.assetmaintenanceservice.dto.service.CreateServiceOptionRequest;
import com.QhomeBase.assetmaintenanceservice.dto.service.ServiceOptionDto;
import com.QhomeBase.assetmaintenanceservice.dto.service.UpdateServiceOptionRequest;
import com.QhomeBase.assetmaintenanceservice.service.ServiceOptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/asset-maintenance")
@RequiredArgsConstructor
public class ServiceOptionController {

    private final ServiceOptionService serviceOptionService;

    @GetMapping("/service-options")
    @PreAuthorize("@authz.canViewServiceConfig()")
    public ResponseEntity<List<ServiceOptionDto>> getAllOptions(@RequestParam(required = false) Boolean isActive) {
        return ResponseEntity.ok(serviceOptionService.getAllOptions(isActive));
    }

    @GetMapping("/services/{serviceId}/options")
    @PreAuthorize("@authz.canViewServiceConfig()")
    public ResponseEntity<List<ServiceOptionDto>> getOptions(@PathVariable UUID serviceId,
                                                             @RequestParam(required = false) Boolean isActive) {
        return ResponseEntity.ok(serviceOptionService.getOptions(serviceId, isActive));
    }

    @GetMapping("/service-options/{optionId}")
    @PreAuthorize("@authz.canViewServiceConfig()")
    public ResponseEntity<ServiceOptionDto> getOption(@PathVariable UUID optionId) {
        return ResponseEntity.ok(serviceOptionService.getOption(optionId));
    }

    @PostMapping("/services/{serviceId}/options")
    @PreAuthorize("@authz.canManageServiceConfig()")
    public ResponseEntity<ServiceOptionDto> createOption(@PathVariable UUID serviceId,
                                                         @Valid @RequestBody CreateServiceOptionRequest request) {
        ServiceOptionDto created = serviceOptionService.createOption(serviceId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/service-options/{optionId}")
    @PreAuthorize("@authz.canManageServiceConfig()")
    public ResponseEntity<ServiceOptionDto> updateOption(@PathVariable UUID optionId,
                                                         @Valid @RequestBody UpdateServiceOptionRequest request) {
        return ResponseEntity.ok(serviceOptionService.updateOption(optionId, request));
    }

    @PutMapping("/service-options/{optionId}/status")
    @PreAuthorize("@authz.canManageServiceConfig()")
    public ResponseEntity<ServiceOptionDto> toggleOptionStatus(@PathVariable UUID optionId,
                                                               @RequestParam("active") boolean active) {
        return ResponseEntity.ok(serviceOptionService.setOptionStatus(optionId, active));
    }

    @DeleteMapping("/service-options/{optionId}")
    @PreAuthorize("@authz.canManageServiceConfig()")
    public ResponseEntity<Void> deleteOption(@PathVariable UUID optionId) {
        serviceOptionService.deleteOption(optionId);
        return ResponseEntity.noContent().build();
    }

}





