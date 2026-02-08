package com.QhomeBase.iamservice.security;

import org.springframework.lang.Nullable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service("authz")
public class AuthzService {
    
    private UserPrincipal principal() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            return null;
        }
        
        var principal = auth.getPrincipal();
        if (principal instanceof UserPrincipal) {
            return (UserPrincipal) principal;
        }
        
        return null;
    }
    
    private boolean hasPerm(String perm) {
        var p = principal();
        return p != null && p.perms() != null && p.perms().contains(perm);
    }

    private boolean hasAnyRole(Set<String> rolesNeed) {
        var p = principal();
        if (p == null || p.roles() == null) return false;
        for (String role : p.roles()) {
            String normalizedRole = normalizeRole(role);
            if (rolesNeed.contains(normalizedRole)) {
                return true;
            }
        }
        return false;
    }
    
    private String normalizeRole(String role) {
        if (role == null) return null;
        return role.toUpperCase();
    }
    
    private boolean isGlobalAdmin() {
        return hasAnyRole(Set.of("ADMIN"));
    }

    public boolean canCreateUser() {
        return hasPerm("iam.user.create");
    }
    
    public boolean canViewUser(UUID userId) {
        var p = principal();
        return hasPerm("iam.user.read") || hasPerm("base.resident.approve") || hasAnyRole(Set.of("ADMIN")) || (p != null && p.uid().equals(userId)) || isGlobalAdmin();
    }
    
    public boolean canViewAllUsers() {
        return hasPerm("iam.user.read") || hasAnyRole(Set.of("ADMIN", "TECHNICIAN", "SUPPORTER")) || isGlobalAdmin();
    }
    
    public boolean canUpdateUser(UUID userId) {
        var p = principal();
        return hasPerm("iam.user.update") || hasAnyRole(Set.of("ADMIN")) || (p != null && p.uid().equals(userId)) || isGlobalAdmin();
    }
    
    public boolean canDeleteUser(UUID userId) {
        var p = principal();
        return hasPerm("iam.user.delete") || hasAnyRole(Set.of("ADMIN")) || (p != null && p.uid().equals(userId)) || isGlobalAdmin();
    }
    
    public boolean canManageUserRoles(UUID userId) {
        return hasPerm("iam.user.role.manage") || hasAnyRole(Set.of("ADMIN", "TECHNICIAN", "SUPPORTER")) || isGlobalAdmin();
    }
    
    public boolean canViewUserPermissions(UUID userId) {
        var p = principal();
        return hasPerm("iam.user.permission.read") || hasAnyRole(Set.of("ADMIN")) || (p != null && p.uid().equals(userId)) || isGlobalAdmin();
    }
    
    public boolean canResetUserPassword(UUID userId) {
        return hasPerm("iam.user.password.reset") || hasAnyRole(Set.of("ADMIN", "TECHNICIAN", "SUPPORTER")) || isGlobalAdmin();
    }
    
    public boolean canLockUserAccount(UUID userId) {
        return hasPerm("iam.user.account.lock") || hasAnyRole(Set.of("ADMIN", "TECHNICIAN", "SUPPORTER")) || isGlobalAdmin();
    }
    
    public boolean canUnlockUserAccount(UUID userId) {
        return hasPerm("iam.user.account.unlock") || hasAnyRole(Set.of("ADMIN", "TECHNICIAN", "SUPPORTER")) || isGlobalAdmin();
    }

    
    public boolean canViewAllRoles() {
        return hasPerm("iam.role.read") || hasAnyRole(Set.of("ADMIN", "TECHNICIAN", "SUPPORTER")) || isGlobalAdmin();
    }
    
    public boolean canCreateRole() {
        return hasPerm("iam.role.create") || hasAnyRole(Set.of("ADMIN")) || isGlobalAdmin();
    }
    
    public boolean canUpdateRole(String roleName) {
        return hasPerm("iam.role.update") || hasAnyRole(Set.of("ADMIN")) || isGlobalAdmin();
    }
    
    public boolean canDeleteRole(String roleName) {
        return hasPerm("iam.role.delete") || hasAnyRole(Set.of("ADMIN")) || isGlobalAdmin();
    }
    
    public boolean canAssignRoleToUser(UUID userId) {
        return hasPerm("iam.role.assign") || hasAnyRole(Set.of("ADMIN", "TECHNICIAN", "SUPPORTER")) || isGlobalAdmin();
    }
    
    public boolean canRemoveRoleFromUser(UUID userId) {
        return hasPerm("iam.role.remove") || hasAnyRole(Set.of("ADMIN", "TECHNICIAN", "SUPPORTER")) || isGlobalAdmin();
    }
    
    public boolean canViewRolePermissions(String roleName) {
        return hasPerm("iam.role.permission.read") || hasAnyRole(Set.of("ADMIN")) || isGlobalAdmin();
    }
    
    public boolean canManageRolePermissions(String roleName) {
        return hasPerm("iam.role.permission.manage") || hasAnyRole(Set.of("ADMIN")) || isGlobalAdmin();
    }

    
    public boolean canViewAllPermissions() {
        return hasPerm("iam.permission.read") || hasAnyRole(Set.of("ADMIN", "TECHNICIAN", "SUPPORTER")) || isGlobalAdmin();
    }
    
    public boolean canCreatePermission() {
        return hasPerm("iam.permission.create") || hasAnyRole(Set.of("ADMIN")) || isGlobalAdmin();
    }
    
    public boolean canUpdatePermission(String permissionCode) {
        return hasPerm("iam.permission.update") || hasAnyRole(Set.of("ADMIN")) || isGlobalAdmin();
    }
    
    public boolean canDeletePermission(String permissionCode) {
        return hasPerm("iam.permission.delete") || hasAnyRole(Set.of("ADMIN")) || isGlobalAdmin();
    }
    
    public boolean canViewPermissionsByService(String servicePrefix) {
        return hasPerm("iam.permission.read") || hasAnyRole(Set.of("ADMIN", "TECHNICIAN", "SUPPORTER")) || isGlobalAdmin();
    }
    
    public boolean canViewPermissionByCode(String permissionCode) {
        return hasPerm("iam.permission.read") || hasAnyRole(Set.of("ADMIN", "TECHNICIAN", "SUPPORTER")) || isGlobalAdmin();
    }
    
    public boolean canLogin() {
        return true;
    }
    
    public boolean canRefreshToken() {
        return true;
    }
    
    public boolean canLogout() {
        return true;
    }
    
    public boolean canChangePassword(UUID userId) {
        var p = principal();
        return hasPerm("iam.user.password.change") || hasAnyRole(Set.of("ADMIN")) || (p != null && p.uid().equals(userId)) || isGlobalAdmin();
    }
    
    public boolean canViewOwnProfile() {
        return true;
    }
    
    public boolean canUpdateOwnProfile() {
        return true;
    }

    
    public boolean canViewSystemStats() {
        return hasPerm("iam.system.stats.read") || hasAnyRole(Set.of("ADMIN")) || isGlobalAdmin();
    }
    
    public boolean canManageSystemSettings() {
        return hasPerm("iam.system.settings.manage") || hasAnyRole(Set.of("ADMIN")) || isGlobalAdmin();
    }
    
    public boolean canViewAuditLogs() {
        return hasPerm("iam.system.audit.read") || hasAnyRole(Set.of("ADMIN", "TECHNICIAN", "SUPPORTER")) || isGlobalAdmin();
    }
    
    public boolean canExportData() {
        return hasPerm("iam.system.data.export") || hasAnyRole(Set.of("ADMIN", "TECHNICIAN", "SUPPORTER")) || isGlobalAdmin();
    }
    
    public boolean canImportData() {
        return hasPerm("iam.system.data.import") || hasAnyRole(Set.of("ADMIN")) || isGlobalAdmin();
    }

    
    public boolean canGenerateTestToken() {
        return hasPerm("iam.test.generate_token") || hasAnyRole(Set.of("ADMIN")) || isGlobalAdmin();
    }
    
    public boolean canAccessTestEndpoints() {
        return hasPerm("iam.test.access") || hasAnyRole(Set.of("ADMIN")) || isGlobalAdmin();
    }
    
    public boolean canViewUserInfo() {
        return true;
    }

    
    public boolean isAdmin() {
        return hasAnyRole(Set.of("ADMIN")) || isGlobalAdmin();
    }
    
    public boolean isTenantOwner() {
        return hasAnyRole(Set.of("ADMIN")) || isGlobalAdmin();
    }
    
    public boolean isTechnician() {
        return hasAnyRole(Set.of("TECHNICIAN"));
    }
    
    public boolean isSupporter() {
        return hasAnyRole(Set.of("SUPPORTER"));
    }
    
    public boolean isAccountant() {
        return hasAnyRole(Set.of("ACCOUNTANT"));
    }
    
    public UUID getCurrentUserId() {
        return principal().uid();
    }

    

    public boolean canAssignEmployeeRole(@Nullable UUID tenantId, UUID employeeId) {
        boolean okRole = hasPerm("iam.employee.role.assign") || hasAnyRole(Set.of("ADMIN", "TECHNICIAN", "SUPPORTER")) || isGlobalAdmin();
        if (hasAnyRole(Set.of("ADMIN")) || isGlobalAdmin()) {
            return true;
        }
        return okRole;
    }
    
    public boolean canRemoveEmployeeRole(@Nullable UUID tenantId, UUID employeeId) {
        boolean okRole = hasPerm("iam.employee.role.remove") || hasAnyRole(Set.of("ADMIN", "TECHNICIAN", "SUPPORTER")) || isGlobalAdmin();
        if (hasAnyRole(Set.of("ADMIN")) || isGlobalAdmin()) {
            return true;
        }
        return okRole;
    }
    
    public boolean canViewEmployeeDetails(@Nullable UUID tenantId, UUID employeeId) {
        var p = principal();
        
        if (hasAnyRole(Set.of("ADMIN")) || isGlobalAdmin()) {
            return true;
        }
        
        if (p != null && p.uid().equals(employeeId)) {
            return true;
        }
        
        boolean okRole = hasPerm("iam.employee.read") || hasAnyRole(Set.of("TECHNICIAN", "SUPPORTER"));
        return okRole;
    }
    
    public boolean canManageDepartment(@Nullable UUID tenantId, @Nullable String department) {
        boolean okRole = hasAnyRole(Set.of("ADMIN", "TECHNICIAN", "SUPPORTER")) || isGlobalAdmin();
        return okRole;
    }
    
    public boolean canBulkAssignRoles(@Nullable UUID tenantId) {
        boolean okRole = hasPerm("iam.employee.role.bulk_assign") || hasAnyRole(Set.of("ADMIN", "TECHNICIAN", "SUPPORTER")) || isGlobalAdmin();
        return okRole;
    }
    
    public boolean canExportEmployeeList(@Nullable UUID tenantId) {
        boolean okRole = hasPerm("iam.employee.export") || hasAnyRole(Set.of("ADMIN", "TECHNICIAN", "SUPPORTER")) || isGlobalAdmin();
        return okRole;
    }
    
    public boolean canImportEmployeeList(@Nullable UUID tenantId) {
        boolean okRole = hasPerm("iam.employee.import") || hasAnyRole(Set.of("ADMIN", "TECHNICIAN", "SUPPORTER")) || isGlobalAdmin();
        return okRole;
    }
    
    public boolean canManageBuilding(@Nullable UUID tenantId) {
        boolean okRole = hasPerm("base.building.manage") || hasAnyRole(Set.of("ADMIN", "TECHNICIAN", "SUPPORTER")) || isGlobalAdmin();
        return okRole;
    }
    
    public boolean canManageUnits(@Nullable UUID tenantId) {
        boolean okRole = hasPerm("base.unit.manage") || hasAnyRole(Set.of("ADMIN", "TECHNICIAN", "SUPPORTER")) || isGlobalAdmin();
        return okRole;
    }
    
    public boolean canManageResidents(@Nullable UUID tenantId) {
        boolean okRole = hasPerm("base.resident.manage") || hasAnyRole(Set.of("ADMIN", "TECHNICIAN", "SUPPORTER")) || isGlobalAdmin();
        return okRole;
    }
    
    public boolean canManageFees(@Nullable UUID tenantId) {
        boolean okRole = hasPerm("finance.fee.manage") || hasAnyRole(Set.of("ADMIN", "TECHNICIAN", "SUPPORTER", "ACCOUNTANT")) || isGlobalAdmin();
        return okRole;
    }
    
    public boolean canApproveResidents(@Nullable UUID tenantId) {
        boolean okRole = hasPerm("base.resident.approve") || hasAnyRole(Set.of("ADMIN", "TECHNICIAN", "SUPPORTER")) || isGlobalAdmin();
        return okRole;
    }
    
    public boolean isBuildingManager() {
        return hasAnyRole(Set.of("ADMIN", "TECHNICIAN", "SUPPORTER")) || isGlobalAdmin();
    }
}
