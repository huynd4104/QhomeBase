package com.QhomeBase.assetmaintenanceservice.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service("authz")
public class AuthzService {
    private UserPrincipal principal() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
    
    private boolean hasPerm(String perm) {
        var p = principal();
        return p.perms() != null && p.perms().contains(perm);
    }

    private boolean hasAnyRole(Set<String> rolesNeed) {
        var p = principal();
        if (p.roles() == null) return false;
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
        String upperRole = role.toUpperCase();
        switch (upperRole) {
            case "MANAGER":
            case "OWNER":
                return "ADMIN";
            default:
                return upperRole;
        }
    }
    
    private boolean isGlobalAdmin() {
        return hasAnyRole(Set.of("ADMIN"));
    }

    public boolean canCreateAsset() {
        boolean okPerm = hasPerm("asset.asset.create");
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER"));
        return okPerm || okRole || isGlobalAdmin();
    }
    
    public boolean canUpdateAsset() {
        boolean okPerm = hasPerm("asset.asset.update");
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER"));
        return okPerm || okRole || isGlobalAdmin();
    }

    public boolean canDeleteAsset() {
        boolean okPerm = hasPerm("asset.asset.delete");
        boolean okRole = hasAnyRole(Set.of("ADMIN"));
        return okPerm || okRole || isGlobalAdmin();
    }

    public boolean canViewAsset() {
        boolean okPerm = hasPerm("asset.asset.view");
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER", "TECHNICIAN", "ACCOUNTANT"));
        return okPerm || okRole || isGlobalAdmin();
    }

    public boolean canViewServiceCategory() {
        boolean okPerm = hasPerm("asset.service-category.view");
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER", "TECHNICIAN", "ACCOUNTANT"));
        return okPerm || okRole || isGlobalAdmin();
    }

    public boolean canManageServiceCategory() {
        boolean okPerm = hasPerm("asset.service-category.manage");
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER"));
        return okPerm || okRole || isGlobalAdmin();
    }

    public boolean canViewServiceConfig() {
        boolean okPerm = hasPerm("asset.service.config.view");
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER", "TECHNICIAN", "ACCOUNTANT"));
        return okPerm || okRole || isGlobalAdmin();
    }

    public boolean canManageServiceConfig() {
        boolean okPerm = hasPerm("asset.service.config.manage");
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER"));
        return okPerm || okRole || isGlobalAdmin();
    }

    public boolean canViewServiceBooking() {
        boolean okPerm = hasPerm("asset.service.booking.view");
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER", "ACCOUNTANT"));
        return okPerm || okRole || isGlobalAdmin();
    }

    public boolean canManageServiceBooking() {
        boolean okPerm = hasPerm("asset.service.booking.manage");
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER"));
        return okPerm || okRole || isGlobalAdmin();
    }

    public boolean canCreateMaintenanceSchedule() {
        boolean okPerm = hasPerm("asset.maintenance.schedule.create");
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER"));
        return okPerm || okRole || isGlobalAdmin();
    }

    public boolean canUpdateMaintenanceSchedule() {
        boolean okPerm = hasPerm("asset.maintenance.schedule.update");
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER"));
        return okPerm || okRole || isGlobalAdmin();
    }

    public boolean canDeleteMaintenanceSchedule() {
        boolean okPerm = hasPerm("asset.maintenance.schedule.delete");
        boolean okRole = hasAnyRole(Set.of("ADMIN"));
        return okPerm || okRole || isGlobalAdmin();
    }

    public boolean canCreateMaintenanceRecord() {
        boolean okPerm = hasPerm("asset.maintenance.record.create");
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER"));
        return okPerm || okRole || isGlobalAdmin();
    }

    public boolean canAssignMaintenance() {
        boolean okPerm = hasPerm("asset.maintenance.assign");
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER"));
        return okPerm || okRole || isGlobalAdmin();
    }

    public boolean canCompleteMaintenance() {
        boolean okPerm = hasPerm("asset.maintenance.complete");
        boolean okRole = hasAnyRole(Set.of("TECHNICIAN", "ADMIN", "SUPPORTER"));
        return okPerm || okRole || isGlobalAdmin();
    }

    public boolean canViewMaintenanceRecords() {
        boolean okPerm = hasPerm("asset.maintenance.record.view");
        boolean okRole = hasAnyRole(Set.of("TECHNICIAN", "ADMIN", "SUPPORTER", "ACCOUNTANT"));
        return okPerm || okRole || isGlobalAdmin();
    }

    public boolean canViewMyTasks() {
        boolean okPerm = hasPerm("asset.maintenance.my-tasks");
        boolean okRole = hasAnyRole(Set.of("TECHNICIAN"));
        return okPerm || okRole;
    }

    public boolean canCreateSupplier() {
        boolean okPerm = hasPerm("asset.supplier.create");
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER"));
        return okPerm || okRole || isGlobalAdmin();
    }

    public boolean canUpdateSupplier() {
        boolean okPerm = hasPerm("asset.supplier.update");
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER"));
        return okPerm || okRole || isGlobalAdmin();
    }

    public boolean canDeleteSupplier() {
        boolean okPerm = hasPerm("asset.supplier.delete");
        boolean okRole = hasAnyRole(Set.of("ADMIN"));
        return okPerm || okRole || isGlobalAdmin();
    }

    public boolean canViewReports() {
        boolean okPerm = hasPerm("asset.report.view");
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER", "ACCOUNTANT"));
        return okPerm || okRole || isGlobalAdmin();
    }
}

