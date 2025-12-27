package com.QhomeBase.assetmaintenanceservice.controller;

import com.QhomeBase.assetmaintenanceservice.dto.supplier.CreateSupplierRequest;
import com.QhomeBase.assetmaintenanceservice.dto.supplier.SupplierResponse;
import com.QhomeBase.assetmaintenanceservice.dto.supplier.UpdateSupplierRequest;
import com.QhomeBase.assetmaintenanceservice.service.SupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/asset-maintenance/suppliers")
@RequiredArgsConstructor
@Tag(name = "Supplier Management", description = "APIs for managing suppliers/vendors")
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping("/{id}")
    @Operation(summary = "Get supplier by ID", description = "Retrieve a supplier by its ID")
    @PreAuthorize("@authz.canViewAsset()")
    public ResponseEntity<SupplierResponse> getSupplierById(@PathVariable UUID id) {
        SupplierResponse response = supplierService.getSupplierById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all suppliers", description = "Retrieve a paginated list of suppliers with optional filters")
    @PreAuthorize("@authz.canViewAsset()")
    public ResponseEntity<Page<SupplierResponse>> getAllSuppliers(
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDir) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDir, sortBy));
        Page<SupplierResponse> response = supplierService.getAllSuppliers(isActive, type, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/active")
    @Operation(summary = "Get all active suppliers", description = "Retrieve a list of all active suppliers")
    @PreAuthorize("@authz.canViewAsset()")
    public ResponseEntity<List<SupplierResponse>> getActiveSuppliers() {
        List<SupplierResponse> response = supplierService.getActiveSuppliers();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/active/by-type")
    @Operation(summary = "Get active suppliers by type", description = "Retrieve a list of active suppliers filtered by type")
    @PreAuthorize("@authz.canViewAsset()")
    public ResponseEntity<List<SupplierResponse>> getActiveSuppliersByType(@RequestParam String type) {
        List<SupplierResponse> response = supplierService.getActiveSuppliersByType(type);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    @Operation(summary = "Search suppliers", description = "Search suppliers by name, contact person, phone, or email")
    @PreAuthorize("@authz.canViewAsset()")
    public ResponseEntity<List<SupplierResponse>> searchSuppliers(
            @RequestParam String query,
            @RequestParam(defaultValue = "20") int limit) {
        List<SupplierResponse> response = supplierService.searchSuppliers(query, limit);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Create supplier", description = "Create a new supplier")
    @PreAuthorize("@authz.canCreateSupplier()")
    public ResponseEntity<SupplierResponse> createSupplier(
            @Valid @RequestBody CreateSupplierRequest request,
            Authentication authentication) {
        SupplierResponse response = supplierService.create(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update supplier", description = "Update an existing supplier")
    @PreAuthorize("@authz.canUpdateSupplier()")
    public ResponseEntity<SupplierResponse> updateSupplier(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSupplierRequest request,
            Authentication authentication) {
        SupplierResponse response = supplierService.update(id, request, authentication);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete supplier", description = "Soft delete a supplier (set isActive to false)")
    @PreAuthorize("@authz.canDeleteSupplier()")
    public ResponseEntity<Void> deleteSupplier(
            @PathVariable UUID id,
            Authentication authentication) {
        supplierService.delete(id, authentication);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/restore")
    @Operation(summary = "Restore supplier", description = "Restore a deleted supplier (set isActive to true)")
    @PreAuthorize("@authz.canUpdateSupplier()")
    public ResponseEntity<SupplierResponse> restoreSupplier(
            @PathVariable UUID id,
            Authentication authentication) {
        SupplierResponse response = supplierService.restore(id, authentication);
        return ResponseEntity.ok(response);
    }
}



