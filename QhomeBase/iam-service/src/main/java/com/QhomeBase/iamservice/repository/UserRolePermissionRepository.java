package com.QhomeBase.iamservice.repository;

import com.QhomeBase.iamservice.model.RolePermission;
import com.QhomeBase.iamservice.model.RolePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {
    // Methods removed because tenant-related tables have been dropped
    // This repository is kept for potential future use but currently has no methods
}
