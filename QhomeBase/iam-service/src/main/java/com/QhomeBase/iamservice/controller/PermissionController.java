package com.QhomeBase.iamservice.controller;

import com.QhomeBase.iamservice.model.Permission;
import com.QhomeBase.iamservice.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    @PreAuthorize("@authz.canViewAllPermissions()")
    public ResponseEntity<List<Permission>> getAllPermissions() {
        List<Permission> permissions = permissionService.getAllPermissions();
        return ResponseEntity.ok(permissions);
    }

    @GetMapping("/{code}")
    @PreAuthorize("@authz.canViewPermissionByCode(#code)")
    public ResponseEntity<Permission> getPermissionByCode(@PathVariable String code) {
        Optional<Permission> permission = permissionService.getPermissionByCode(code);
        return permission.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/service/{servicePrefix}")
    @PreAuthorize("@authz.canViewPermissionsByService(#servicePrefix)")
    public ResponseEntity<List<Permission>> getPermissionsByService(@PathVariable String servicePrefix) {
        List<Permission> permissions = permissionService.getPermissionsByService(servicePrefix);
        return ResponseEntity.ok(permissions);
    }

    @GetMapping("/base")
    @PreAuthorize("@authz.canViewPermissionsByService('base')")
    public ResponseEntity<List<Permission>> getBaseServicePermissions() {
        List<Permission> permissions = permissionService.getBaseServicePermissions();
        return ResponseEntity.ok(permissions);
    }

    @GetMapping("/iam")
    @PreAuthorize("@authz.canViewPermissionsByService('iam')")
    public ResponseEntity<List<Permission>> getIamServicePermissions() {
        List<Permission> permissions = permissionService.getIamServicePermissions();
        return ResponseEntity.ok(permissions);
    }

    @GetMapping("/maintenance")
    @PreAuthorize("@authz.canViewPermissionsByService('maintenance')")
    public ResponseEntity<List<Permission>> getMaintenanceServicePermissions() {
        List<Permission> permissions = permissionService.getMaintenanceServicePermissions();
        return ResponseEntity.ok(permissions);
    }

    @GetMapping("/finance")
    @PreAuthorize("@authz.canViewPermissionsByService('finance')")
    public ResponseEntity<List<Permission>> getFinanceServicePermissions() {
        List<Permission> permissions = permissionService.getFinanceServicePermissions();
        return ResponseEntity.ok(permissions);
    }

    @GetMapping("/document")
    @PreAuthorize("@authz.canViewPermissionsByService('document')")
    public ResponseEntity<List<Permission>> getDocumentServicePermissions() {
        List<Permission> permissions = permissionService.getDocumentServicePermissions();
        return ResponseEntity.ok(permissions);
    }

    @GetMapping("/report")
    @PreAuthorize("@authz.canViewPermissionsByService('report')")
    public ResponseEntity<List<Permission>> getReportServicePermissions() {
        List<Permission> permissions = permissionService.getReportServicePermissions();
        return ResponseEntity.ok(permissions);
    }

    @GetMapping("/system")
    @PreAuthorize("@authz.canViewPermissionsByService('system')")
    public ResponseEntity<List<Permission>> getSystemServicePermissions() {
        List<Permission> permissions = permissionService.getSystemServicePermissions();
        return ResponseEntity.ok(permissions);
    }

    @PostMapping
    @PreAuthorize("@authz.canCreatePermission()")
    public ResponseEntity<Permission> createPermission(@RequestBody Permission permission) {
        Permission createdPermission = permissionService.createPermission(
                permission.getCode(), 
                permission.getDescription()
        );
        return ResponseEntity.ok(createdPermission);
    }

    @PutMapping("/{code}")
    @PreAuthorize("@authz.canUpdatePermission(#code)")
    public ResponseEntity<Permission> updatePermission(
            @PathVariable String code, 
            @RequestBody Permission permission) {
        try {
            Permission updatedPermission = permissionService.updatePermission(code, permission.getDescription());
            return ResponseEntity.ok(updatedPermission);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{code}")
    @PreAuthorize("@authz.canDeletePermission(#code)")
    public ResponseEntity<Void> deletePermission(@PathVariable String code) {
        try {
            permissionService.deletePermission(code);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
