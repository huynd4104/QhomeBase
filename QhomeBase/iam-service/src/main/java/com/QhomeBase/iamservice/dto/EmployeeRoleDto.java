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
public class EmployeeRoleDto {
    
    private UUID userId;
    private String username;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String department;
    private String position;
    private UUID tenantId;
    private String tenantName;
    private List<RoleAssignmentDto> assignedRoles;
    private List<String> allPermissions;
    private Instant createdAt;
    private Instant updatedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleAssignmentDto {
        private String roleName;
        private String roleDescription;
        private Instant assignedAt;
        private String assignedBy;
        private boolean isActive;
    }
}
