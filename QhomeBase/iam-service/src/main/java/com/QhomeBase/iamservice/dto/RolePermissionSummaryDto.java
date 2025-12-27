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
public class RolePermissionSummaryDto {
    private String role;
    private UUID tenantId;
    private List<String> grantedPermissions;
    private List<String> deniedPermissions;
    private List<String> effectivePermissions;
    private int totalPermissions;
}
