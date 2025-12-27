package com.QhomeBase.iamservice.controller;

import com.QhomeBase.iamservice.dto.*;
import com.QhomeBase.iamservice.service.UserGrantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/user-permissions")
@RequiredArgsConstructor
public class UserPermissionController {

    private final UserGrantService userGrantService;

    @PostMapping("/grant/{tenantId}/{userId}")
    @PreAuthorize("@authz.canManagePermissions(#tenantId)")
    public ResponseEntity<Void> grantPermissionsToUser(
            @PathVariable UUID tenantId,
            @PathVariable UUID userId,
            @RequestBody UserPermissionGrantRequest request,
            Authentication authentication) {
        request.setTenantId(tenantId);
        request.setUserId(userId);
        
        userGrantService.grantPermissionsToUser(request, authentication);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/deny/{tenantId}/{userId}")
    @PreAuthorize("@authz.canManagePermissions(#tenantId)")
    public ResponseEntity<Void> denyPermissionsFromUser(
            @PathVariable UUID tenantId,
            @PathVariable UUID userId,
            @RequestBody UserPermissionDenyRequest request,
            Authentication authentication) {
        request.setTenantId(tenantId);
        request.setUserId(userId);
        
        userGrantService.denyPermissionsFromUser(request, authentication);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/revoke-grants/{tenantId}/{userId}")
    @PreAuthorize("@authz.canManagePermissions(#tenantId)")
    public ResponseEntity<Void> revokeGrantsFromUser(
            @PathVariable UUID tenantId,
            @PathVariable UUID userId,
            @RequestBody UserPermissionRevokeRequest request) {
        request.setTenantId(tenantId);
        request.setUserId(userId);
        
        userGrantService.revokeGrantsFromUser(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/revoke-denies/{tenantId}/{userId}")
    @PreAuthorize("@authz.canManagePermissions(#tenantId)")
    public ResponseEntity<Void> revokeDeniesFromUser(
            @PathVariable UUID tenantId,
            @PathVariable UUID userId,
            @RequestBody UserPermissionRevokeRequest request) {
        request.setTenantId(tenantId);
        request.setUserId(userId);
        
        userGrantService.revokeDeniesFromUser(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/summary/{tenantId}/{userId}")
    @PreAuthorize("@authz.canManagePermissions(#tenantId)")
    public ResponseEntity<UserPermissionSummaryDto> getUserPermissionSummary(
            @PathVariable UUID tenantId,
            @PathVariable UUID userId) {
        UserPermissionSummaryDto summary = userGrantService.getUserPermissionSummary(userId, tenantId);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/grants/{tenantId}/{userId}")
    @PreAuthorize("@authz.canManagePermissions(#tenantId)")
    public ResponseEntity<List<String>> getActiveGrants(
            @PathVariable UUID tenantId,
            @PathVariable UUID userId) {
        List<String> grants = userGrantService.getActiveGrants(userId, tenantId);
        return ResponseEntity.ok(grants);
    }

    @GetMapping("/denies/{tenantId}/{userId}")
    @PreAuthorize("@authz.canManagePermissions(#tenantId)")
    public ResponseEntity<List<String>> getActiveDenies(
            @PathVariable UUID tenantId,
            @PathVariable UUID userId) {
        List<String> denies = userGrantService.getActiveDenies(userId, tenantId);
        return ResponseEntity.ok(denies);
    }
}
