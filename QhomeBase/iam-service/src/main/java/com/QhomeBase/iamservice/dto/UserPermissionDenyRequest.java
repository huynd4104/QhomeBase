package com.QhomeBase.iamservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPermissionDenyRequest {
    private UUID userId;
    private UUID tenantId;
    private List<String> permissionCodes;
    private Instant expiresAt;
    private String reason;
}




