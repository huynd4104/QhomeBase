package com.QhomeBase.iamservice.service;

import com.QhomeBase.iamservice.model.Permission;
import com.QhomeBase.iamservice.model.RolePermission;
import com.QhomeBase.iamservice.model.RolePermissionId;
import com.QhomeBase.iamservice.repository.PermissionRepository;
import com.QhomeBase.iamservice.repository.RolePermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RolePermissionService {
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;

    @Transactional
    public void addPermissionToRole(String role, String permissionCode) {
        String normalizedRole = role.toUpperCase();
        if (rolePermissionRepository.existsByRoleAndPermissionCode(normalizedRole, permissionCode)) {
            throw new IllegalArgumentException("Permission already exists in this role");
        }
        
        if (!permissionRepository.existsById(permissionCode)) {
            throw new IllegalArgumentException("Permission not found: " + permissionCode);
        }
        
        var rolePermission = new RolePermission();
        var rolePermissionId = new RolePermissionId(normalizedRole, permissionCode);
        rolePermission.setRolePermissionId(rolePermissionId);
        
        rolePermissionRepository.save(rolePermission);
    }

    @Transactional
    public void removePermissionFromRole(String role, String permissionCode) {
        String normalizedRole = role.toUpperCase();
        if (!rolePermissionRepository.existsByRoleAndPermissionCode(normalizedRole, permissionCode)) {
            throw new IllegalArgumentException("Permission not found in this role");
        }
        
        var rolePermissionId = new RolePermissionId(normalizedRole, permissionCode);
        rolePermissionRepository.deleteById(rolePermissionId);
    }

    @Transactional(readOnly = true)
    public List<Permission> getPermissionsByRole(String role) {
        String normalizedRole = role.toUpperCase();
        return rolePermissionRepository.findPermissionObjectsByRole(normalizedRole);
    }

    @Transactional(readOnly = true)
    public boolean hasPermission(String role, String permissionCode) {
        String normalizedRole = role.toUpperCase();
        return rolePermissionRepository.existsByRoleAndPermissionCode(normalizedRole, permissionCode);
    }

    @Transactional
    public void addMultiplePermissionsToRole(String role, List<String> permissionCodes) {
        String normalizedRole = role.toUpperCase();
        for (String permissionCode : permissionCodes) {
            if (!rolePermissionRepository.existsByRoleAndPermissionCode(normalizedRole, permissionCode)) {
                if (permissionRepository.existsById(permissionCode)) {
                    var rolePermission = new RolePermission();
                    var rolePermissionId = new RolePermissionId(normalizedRole, permissionCode);
                    rolePermission.setRolePermissionId(rolePermissionId);
                    rolePermissionRepository.save(rolePermission);
                }
            }
        }
    }

    @Transactional
    public void removeMultiplePermissionsFromRole(String role, List<String> permissionCodes) {
        String normalizedRole = role.toUpperCase();
        for (String permissionCode : permissionCodes) {
            if (rolePermissionRepository.existsByRoleAndPermissionCode(normalizedRole, permissionCode)) {
                var rolePermissionId = new RolePermissionId(normalizedRole, permissionCode);
                rolePermissionRepository.deleteById(rolePermissionId);
            }
        }
    }

    @Transactional
    public void updateRolePermissions(String role, List<String> permissionCodes) {
        String normalizedRole = role.toUpperCase();
        List<Permission> currentPermissions = rolePermissionRepository.findPermissionObjectsByRole(normalizedRole);
        List<String> currentPermissionCodes = currentPermissions.stream()
                .map(Permission::getCode)
                .toList();
        
        List<String> toAdd = permissionCodes.stream()
                .filter(code -> !currentPermissionCodes.contains(code))
                .toList();
        
        List<String> toRemove = currentPermissionCodes.stream()
                .filter(code -> !permissionCodes.contains(code))
                .toList();
        
        addMultiplePermissionsToRole(normalizedRole, toAdd);
        removeMultiplePermissionsFromRole(normalizedRole, toRemove);
    }
}
