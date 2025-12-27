package com.QhomeBase.customerinteractionservice.security;

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
        return role.toUpperCase();
    }
    
    private boolean isGlobalAdmin() {
        return hasAnyRole(Set.of("ADMIN"));
    }

    public boolean canCreateBuilding() {
        boolean okRole = hasAnyRole(Set.of("ADMIN", "TECHNICIAN", "SUPPORTER"));
        boolean okPerm = hasPerm("base.building.create");
        return okRole || okPerm || isGlobalAdmin();
    }
    
    public boolean canUpdateBuilding() {
        boolean okRole = hasAnyRole(Set.of("ADMIN", "TECHNICIAN", "SUPPORTER"));
        boolean okPerm = hasPerm("base.building.update");
        return okRole || okPerm || isGlobalAdmin();
    }
    public boolean canRequestDeleteBuilding() {
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER"));
        boolean okPerm = hasPerm("base.building.delete.request");
        return okRole || okPerm || isGlobalAdmin();
    }

    public boolean canApproveDeleteBuilding() {
        boolean okRole = hasAnyRole(Set.of("ADMIN"));
        boolean okPerm = hasPerm("base.building.delete.approve");
        return okRole || okPerm || isGlobalAdmin();
    }

    public boolean canDeleteBuilding() {
        boolean okRole = hasAnyRole(Set.of("ADMIN"));
        boolean okPerm = hasPerm("base.building.delete");
        return okRole || okPerm || isGlobalAdmin();
    }

    public boolean canCompleteBuildingDeletion() {
        boolean okRole = hasAnyRole(Set.of("ADMIN"));
        boolean okPerm = hasPerm("base.building.delete.approve");
        return okRole || okPerm || isGlobalAdmin();
    }

    public boolean canRejectDeleteBuilding() {
        boolean okRole = hasAnyRole(Set.of("ADMIN"));
        boolean okPerm = hasPerm("base.building.delete.approve");
        return okRole || okPerm || isGlobalAdmin();
    }

    public boolean canViewDeleteBuilding(UUID id) {
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER"));
        boolean okPerm = hasPerm("base.building.delete.request") || hasPerm("base.building.delete.approve");
        return okRole || okPerm || isGlobalAdmin();
    }

    public boolean canViewAllDeleteBuildings() {
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER"));
        boolean okPerm = hasPerm("base.building.delete.approve");
        return okRole || okPerm || isGlobalAdmin();
    }
    public boolean canCreateUnit() {
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


    public boolean canCreateVehicle() {
        boolean okRole = hasAnyRole(Set.of("ADMIN"));
        return okRole || isGlobalAdmin();
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

    public boolean canCreateVehicleRegistration() {
        boolean okRole = hasAnyRole(Set.of("ADMIN"));
        return okRole || isGlobalAdmin();
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


    public boolean canCreateNews() {
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER"));
        boolean okPerm = hasPerm("content.news.create");
        return okRole || okPerm || isGlobalAdmin();
    }

    public boolean canUpdateNews() {
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER"));
        boolean okPerm = hasPerm("content.news.update");
        return okRole || okPerm || isGlobalAdmin();
    }

    public boolean canDeleteNews() {
        boolean okRole = hasAnyRole(Set.of("ADMIN"));
        boolean okPerm = hasPerm("content.news.delete");
        return okRole || okPerm || isGlobalAdmin();
    }

    public boolean canViewNews() {
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER", "TECHNICIAN"));
        boolean okPerm = hasPerm("content.news.view");
        return okRole || okPerm || isGlobalAdmin();
    }

    public boolean canPublishNews() {
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER"));
        boolean okPerm = hasPerm("content.news.publish");
        return okRole || okPerm || isGlobalAdmin();
    }

    public boolean canViewAllNews() {
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER"));
        boolean okPerm = hasPerm("content.news.view");
        return okRole || okPerm || isGlobalAdmin();
    }

    public boolean canReadNews() {
        boolean okRole = hasAnyRole(Set.of("RESIDENT", "UNIT_OWNER"));
        return okRole;
    }

    public boolean canUploadNewsImage() {
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER"));
        boolean okPerm = hasPerm("content.news.image.upload");
        return okRole || okPerm || isGlobalAdmin();
    }

    public boolean canDeleteNewsImage() {
        boolean okRole = hasAnyRole(Set.of("ADMIN"));
        boolean okPerm = hasPerm("content.news.image.delete");
        return okRole || okPerm || isGlobalAdmin();
    }

    public boolean canUpdateNewsImage() {
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER"));
        boolean okPerm = hasPerm("content.news.image.update");
        return okRole || okPerm || isGlobalAdmin();
    }

    public boolean canManageNotifications() {
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER", "TECHNICIAN"));
        boolean okPerm = hasPerm("content.notification.manage");
        return okRole || okPerm || isGlobalAdmin();
    }

    public boolean canViewNotifications() {
        boolean okRole = hasAnyRole(Set.of("ADMIN", "SUPPORTER", "TECHNICIAN", "UNIT_OWNER", "RESIDENT"));
        boolean okPerm = hasPerm("content.notification.view");
        return okRole || okPerm || isGlobalAdmin();
    }

}
