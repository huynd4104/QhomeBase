package com.QhomeBase.iamservice.service;

import com.QhomeBase.iamservice.model.Permission;
import com.QhomeBase.iamservice.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;

    public List<Permission> getAllPermissions() {
        return permissionRepository.findAll();
    }

    public Optional<Permission> getPermissionByCode(String code) {
        return permissionRepository.findById(code);
    }

    public List<Permission> getPermissionsByService(String servicePrefix) {
        return permissionRepository.findAll().stream()
                .filter(permission -> permission.getCode().startsWith(servicePrefix + "."))
                .collect(Collectors.toList());
    }

    public List<Permission> getBaseServicePermissions() {
        return permissionRepository.findAll().stream()
                .filter(Permission::isBaseService)
                .collect(Collectors.toList());
    }

    public List<Permission> getIamServicePermissions() {
        return permissionRepository.findAll().stream()
                .filter(Permission::isIamService)
                .collect(Collectors.toList());
    }

    public List<Permission> getMaintenanceServicePermissions() {
        return permissionRepository.findAll().stream()
                .filter(Permission::isMaintenanceService)
                .collect(Collectors.toList());
    }

    public List<Permission> getFinanceServicePermissions() {
        return permissionRepository.findAll().stream()
                .filter(Permission::isFinanceService)
                .collect(Collectors.toList());
    }

    public List<Permission> getDocumentServicePermissions() {
        return permissionRepository.findAll().stream()
                .filter(Permission::isDocumentService)
                .collect(Collectors.toList());
    }

    public List<Permission> getReportServicePermissions() {
        return permissionRepository.findAll().stream()
                .filter(Permission::isReportService)
                .collect(Collectors.toList());
    }

    public List<Permission> getSystemServicePermissions() {
        return permissionRepository.findAll().stream()
                .filter(Permission::isSystemService)
                .collect(Collectors.toList());
    }

    public Permission createPermission(String code, String description) {
        Permission permission = new Permission();
        permission.setCode(code);
        permission.setDescription(description);
        return permissionRepository.save(permission);
    }

    public Permission updatePermission(String code, String description) {
        Optional<Permission> existingPermission = permissionRepository.findById(code);
        if (existingPermission.isPresent()) {
            Permission permission = existingPermission.get();
            permission.setDescription(description);
            return permissionRepository.save(permission);
        }
        throw new IllegalArgumentException("Permission with code " + code + " not found");
    }

    public void deletePermission(String code) {
        permissionRepository.deleteById(code);
    }

    public boolean permissionExists(String code) {
        return permissionRepository.existsById(code);
    }
}

