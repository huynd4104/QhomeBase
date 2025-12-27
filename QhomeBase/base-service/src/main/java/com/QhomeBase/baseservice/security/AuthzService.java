package com.QhomeBase.baseservice.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

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
            case "UNIT_OWNER":
                return "UNIT_OWNER";
            default:
                return upperRole;
        }
    }
    
    private boolean isGlobalAdmin() {
        return hasAnyRole(Set.of("ADMIN"));
    }

    // ========== Building Permissions ==========
    
    public boolean canCreateBuilding() {
        boolean okPerm = hasPerm("base.building.create");
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER"));
        return okPerm || okRole || isGlobalAdmin();
    }
    
    public boolean canUpdateBuilding() {
        boolean okPerm = hasPerm("base.building.update");
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER"));
        return okPerm || okRole || isGlobalAdmin();
    }
    
    public boolean canRequestDeleteBuilding(UUID buildingId) {
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER"));
        boolean okPerm = hasPerm("base.building.delete.request");
        return okRole || okPerm || isGlobalAdmin();
    }

    public boolean canApproveDeleteBuilding(UUID buildingId) {
        boolean okRole = hasAnyRole(Set.of("ADMIN"));
        boolean okPerm = hasPerm("base.building.delete.approve");
        return okRole || okPerm || isGlobalAdmin();
    }

    public boolean canDeleteBuilding() {
        boolean okPerm = hasPerm("base.building.delete");
        boolean okRole = hasAnyRole(Set.of("ADMIN"));
        return okPerm || okRole || isGlobalAdmin();
    }

    public boolean canApproveBuildingDeletion() {
        boolean okRole = hasAnyRole(Set.of("ADMIN"));
        boolean okPerm = hasPerm("base.building.delete.approve");
        return okRole || okPerm || isGlobalAdmin();
    }

    public boolean canCompleteBuildingDeletion() {
        boolean okRole = hasAnyRole(Set.of("ADMIN"));
        boolean okPerm = hasPerm("base.building.delete.approve");
        return okRole || okPerm || isGlobalAdmin();
    }

    public boolean canRejectDeleteBuilding(UUID buildingId) {
        boolean okRole = hasAnyRole(Set.of("ADMIN"));
        boolean okPerm = hasPerm("base.building.delete.approve");
        return okRole || okPerm || isGlobalAdmin();
    }

    public boolean canViewDeleteBuilding(UUID id) {
        boolean okPerm = hasPerm("base.building.delete.request") || hasPerm("base.building.delete.approve");
        return okPerm;
    }

    public boolean canViewAllDeleteBuildings() {
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER"));
        boolean okPerm = hasPerm("base.building.delete.approve");
        return okRole || okPerm || isGlobalAdmin();
    }

    // ========== Unit Permissions ==========
    
    public boolean canCreateUnit(UUID buildingId) {
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER"));
        boolean okPerm = hasPerm("base.unit.create");
        return okRole || okPerm || isGlobalAdmin();
    }

    public boolean canUpdateUnit(UUID unitId) {
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER", "UNIT_OWNER"));
        boolean okPerm = hasPerm("base.unit.update");
        return okRole || okPerm || isGlobalAdmin();
    }

    public boolean canViewUnit(UUID unitId) {
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER", "UNIT_OWNER", "RESIDENT"));
        boolean okPerm = hasPerm("base.unit.view");
        return okRole || okPerm || isGlobalAdmin();
    }

    public boolean canViewUnits() {
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER"));
        boolean okPerm = hasPerm("base.unit.view");
        return okRole || okPerm || isGlobalAdmin();
    }

    public boolean canDeleteUnit(UUID unitId) {
        boolean okRole = hasAnyRole(Set.of("ADMIN"));
        boolean okPerm = hasPerm("base.unit.delete");
        return okRole || okPerm || isGlobalAdmin();
    }

    public boolean canManageUnitStatus(UUID unitId) {
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER"));
        boolean okPerm = hasPerm("base.unit.status.manage");
        return okRole || okPerm || isGlobalAdmin();
    }

    public boolean canViewUnitsByBuilding(UUID buildingId) {
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER"));
        boolean okPerm = hasPerm("base.unit.view");
        return okRole || okPerm || isGlobalAdmin();
    }

    // ========== Vehicle Permissions ==========

    public boolean canCreateVehicle() {
        return true; // Any authenticated user can create vehicle
    }

    public boolean canUpdateVehicle(UUID vehicleId) {
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER", "UNIT_OWNER", "RESIDENT"));
        boolean okPerm = hasPerm("base.vehicle.update");
        return okRole || okPerm || isGlobalAdmin();
    }

    public boolean canViewVehicle(UUID vehicleId) {
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER", "UNIT_OWNER", "RESIDENT"));
        boolean okPerm = hasPerm("base.vehicle.view");
        return okRole || okPerm || isGlobalAdmin();
    }

    public boolean canViewVehicles() {
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER"));
        boolean okPerm = hasPerm("base.vehicle.view");
        return okRole || okPerm || isGlobalAdmin();
    }

    public boolean canDeleteVehicle(UUID vehicleId) {
        boolean okRole = hasAnyRole(Set.of("ADMIN"));
        boolean okPerm = hasPerm("base.vehicle.delete");
        return okRole || okPerm || isGlobalAdmin();
    }

    public boolean canManageVehicleStatus(UUID vehicleId) {
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER"));
        boolean okPerm = hasPerm("base.vehicle.status.manage");
        return okRole || okPerm || isGlobalAdmin();
    }

    public boolean canViewVehiclesByResident(UUID residentId) {
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER", "UNIT_OWNER", "RESIDENT"));
        boolean okPerm = hasPerm("base.vehicle.view");
        return okRole || okPerm || isGlobalAdmin();
    }

    public boolean canViewVehiclesByUnit(UUID unitId) {
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER", "UNIT_OWNER"));
        boolean okPerm = hasPerm("base.vehicle.view");
        return okRole || okPerm || isGlobalAdmin();
    }

    // ========== Vehicle Registration Permissions ==========
    
    public boolean canCreateVehicleRegistration() {
        return true; // Any authenticated user can request vehicle registration
    }

    public boolean canApproveVehicleRegistration(UUID requestId) {
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER"));
        boolean okPerm = hasPerm("base.vehicle.registration.approve");
        return okRole || okPerm || isGlobalAdmin();
    }

    public boolean canViewVehicleRegistration(UUID requestId) {
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER", "UNIT_OWNER", "RESIDENT"));
        boolean okPerm = hasPerm("base.vehicle.registration.view");
        return okRole || okPerm || isGlobalAdmin();
    }

    public boolean canViewVehicleRegistrations() {
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER"));
        boolean okPerm = hasPerm("base.vehicle.registration.view");
        return okRole || okPerm || isGlobalAdmin();
    }

    public boolean canViewVehicleRegistrationsByResident(UUID residentId) {
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER", "UNIT_OWNER", "RESIDENT"));
        boolean okPerm = hasPerm("base.vehicle.registration.view");
        return okRole || okPerm || isGlobalAdmin();
    }

    public boolean canViewVehicleRegistrationsByUnit(UUID unitId) {
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER", "UNIT_OWNER"));
        boolean okPerm = hasPerm("base.vehicle.registration.view");
        return okRole || okPerm || isGlobalAdmin();
    }

    public boolean canViewAllVehicleRegistrations() {
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER"));
        boolean okPerm = hasPerm("base.vehicle.registration.view");
        return okRole || okPerm || isGlobalAdmin();
    }

    public boolean canCancelVehicleRegistration(UUID requestId) {
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER", "UNIT_OWNER", "RESIDENT"));
        boolean okPerm = hasPerm("base.vehicle.registration.cancel");
        return okRole || okPerm || isGlobalAdmin();
    }

    // ========== Service Request Permissions ==========

    public boolean canManageServiceRequests() {
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER"));
        boolean okPerm = hasPerm("base.service-request.manage");
        return okRole || okPerm || isGlobalAdmin();
    }
}
