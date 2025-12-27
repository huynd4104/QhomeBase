package com.QhomeBase.iamservice.service;

import com.QhomeBase.iamservice.dto.*;
import com.QhomeBase.iamservice.model.User;
import com.QhomeBase.iamservice.model.UserRole;
import com.QhomeBase.iamservice.repository.RolePermissionRepository;
import com.QhomeBase.iamservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserGrantService {
    private final UserRepository userRepository;
    private final RolePermissionRepository rolePermissionRepository;

    @Deprecated
    @Transactional
    public void grantPermissionsToUser(UserPermissionGrantRequest request, Authentication authentication) {
        throw new UnsupportedOperationException("Tenant-based permission grants are no longer supported. Use role-based permissions instead.");
    }

    @Deprecated
    @Transactional
    public void denyPermissionsFromUser(UserPermissionDenyRequest request, Authentication authentication) {
        throw new UnsupportedOperationException("Tenant-based permission denies are no longer supported. Use role-based permissions instead.");
    }

    @Deprecated
    @Transactional
    public void revokeGrantsFromUser(UserPermissionRevokeRequest request) {
        throw new UnsupportedOperationException("Tenant-based permission grants are no longer supported.");
    }

    @Deprecated
    @Transactional
    public void revokeDeniesFromUser(UserPermissionRevokeRequest request) {
        throw new UnsupportedOperationException("Tenant-based permission denies are no longer supported.");
    }

    public UserPermissionSummaryDto getUserPermissionSummary(UUID userId, UUID tenantId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        List<UserRole> userRoles = user.getRoles();

        Set<String> permissionsSet = new HashSet<>();
        if (userRoles != null && !userRoles.isEmpty()) {
            for (UserRole role : userRoles) {
                List<String> rolePerms = rolePermissionRepository.findPermissionCodesByRole(role.name());
                permissionsSet.addAll(rolePerms);
            }
        }
        List<String> effectivePermissions = new ArrayList<>(permissionsSet);

        List<UserPermissionOverrideDto> grants = new ArrayList<>();
        List<UserPermissionOverrideDto> denies = new ArrayList<>();
        List<String> inheritedPermissions = new ArrayList<>(effectivePermissions);

        return UserPermissionSummaryDto.builder()
                .userId(userId)
                .tenantId(null)
                .grants(grants)
                .denies(denies)
                .totalGrants(0)
                .totalDenies(0)
                .activeGrants(0)
                .activeDenies(0)
                .temporaryGrants(0)
                .temporaryDenies(0)
                .inheritedFromRoles(inheritedPermissions)
                .grantedPermissions(List.of())
                .deniedPermissions(List.of())
                .effectivePermissions(effectivePermissions)
                .totalEffectivePermissions(effectivePermissions.size())
                .build();
    }

    @Deprecated
    public List<String> getActiveGrants(UUID userId, UUID tenantId) {
        return List.of();
    }

    @Deprecated
    public List<String> getActiveDenies(UUID userId, UUID tenantId) {
        return List.of();
    }

    private Instant convertToInstant(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Instant) {
            return (Instant) obj;
        }
        if (obj instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) obj).toInstant();
        }
        throw new IllegalArgumentException("Cannot convert " + obj.getClass() + " to Instant");
    }
}
