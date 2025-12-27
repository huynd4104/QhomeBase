package com.QhomeBase.iamservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPermissionOverrideDto {
    private String permissionCode;
    private String permissionDescription;
    private boolean granted;
    private Instant grantedAt;
    private String grantedBy;
    private Instant expiresAt;
    private boolean isExpired;
    private boolean isTemporary;
    private String reason;
}

