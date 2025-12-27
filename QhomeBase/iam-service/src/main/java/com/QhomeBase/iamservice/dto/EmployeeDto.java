package com.QhomeBase.iamservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDto {
    private UUID userId;
    private String username;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String department;
    private String position;
    private UUID tenantId;
    private String tenantName;
    private boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
    
    private EmployeePermissionStatus permissionStatus;
    private int grantedOverrides;
    private int deniedOverrides;
    private int totalOverrides;
    private boolean hasTemporaryPermissions;
    private Instant lastPermissionChange;
}
