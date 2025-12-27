package com.QhomeBase.iamservice.service;

import com.QhomeBase.iamservice.model.Permission;
import com.QhomeBase.iamservice.model.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {
    
    private final RolePermissionService rolePermissionService;
    
    @Transactional(readOnly = true)
    public List<UserRole> getAllRoles() {
        return Arrays.asList(UserRole.values());
    }
    
    @Transactional(readOnly = true)
    public List<Permission> getPermissionsByRole(UserRole role) {
        return rolePermissionService.getPermissionsByRole(role.name());
    }
    
    @Transactional(readOnly = true)
    public List<Permission> getAccountantPermissions() {
        return rolePermissionService.getPermissionsByRole(UserRole.ACCOUNTANT.name());
    }
    
    @Transactional(readOnly = true)
    public List<Permission> getAdminPermissions() {
        return rolePermissionService.getPermissionsByRole(UserRole.ADMIN.name());
    }
    
    @Transactional(readOnly = true)
    public List<Permission> getTechnicianPermissions() {
        return rolePermissionService.getPermissionsByRole(UserRole.TECHNICIAN.name());
    }
    
    @Transactional(readOnly = true)
    public List<Permission> getSupporterPermissions() {
        return rolePermissionService.getPermissionsByRole(UserRole.SUPPORTER.name());
    }
}


