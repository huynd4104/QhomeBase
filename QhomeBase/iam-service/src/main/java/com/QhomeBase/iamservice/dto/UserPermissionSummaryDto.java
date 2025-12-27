package com.QhomeBase.iamservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPermissionSummaryDto {
    private UUID userId;
    private UUID tenantId;
    
    private List<UserPermissionOverrideDto> grants;
    private List<UserPermissionOverrideDto> denies;
    
    private int totalGrants;
    private int totalDenies;
    private int activeGrants;
    private int activeDenies;
    private int temporaryGrants;
    private int temporaryDenies;
    
   
    private List<String> inheritedFromRoles;   // Permissions from roles
    private List<String> grantedPermissions;   // Direct grants (permission codes)
    private List<String> deniedPermissions;    // Direct denies (permission codes)
    
    private List<String> effectivePermissions;
    private int totalEffectivePermissions;
}




