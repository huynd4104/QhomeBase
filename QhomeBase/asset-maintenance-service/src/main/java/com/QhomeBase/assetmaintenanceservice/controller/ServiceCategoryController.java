package com.QhomeBase.assetmaintenanceservice.controller;

import com.QhomeBase.assetmaintenanceservice.dto.service.ServiceCategoryDto;
import com.QhomeBase.assetmaintenanceservice.service.ServiceCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/asset-maintenance/service-categories")
@RequiredArgsConstructor
public class ServiceCategoryController {

    private final ServiceCategoryService serviceCategoryService;

    @GetMapping
    @PreAuthorize("@authz.canViewServiceCategory()")
    public ResponseEntity<List<ServiceCategoryDto>> getAll() {
        return ResponseEntity.ok(serviceCategoryService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.canViewServiceCategory()")
    public ResponseEntity<ServiceCategoryDto> getById(@PathVariable UUID id) {
        return serviceCategoryService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("@authz.canManageServiceCategory()")
    public ResponseEntity<ServiceCategoryDto> create(@Valid @RequestBody ServiceCategoryDto request) {
        ServiceCategoryDto created = serviceCategoryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.canManageServiceCategory()")
    public ResponseEntity<ServiceCategoryDto> update(@PathVariable UUID id,
                                                     @Valid @RequestBody ServiceCategoryDto request) {
        return ResponseEntity.ok(serviceCategoryService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@authz.canManageServiceCategory()")
    public ResponseEntity<ServiceCategoryDto> toggleStatus(@PathVariable UUID id,
                                                           @RequestParam("active") boolean active) {
        return ResponseEntity.ok(serviceCategoryService.setActive(id, active));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.canManageServiceCategory()")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        serviceCategoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}


