package com.QhomeBase.iamservice.repository;

import com.QhomeBase.iamservice.model.Permission;
import com.QhomeBase.iamservice.model.RolePermission;
import com.QhomeBase.iamservice.model.RolePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {

    @Query(value = """
        SELECT p.code, p.description
        FROM iam.permissions p
        JOIN iam.role_permissions rp ON p.code = rp.permission_code
        WHERE rp.role = :role
        """, nativeQuery = true)
    List<Object[]> findPermissionsByRole(@Param("role") String role);

    @Query(value = """
        SELECT p.code, p.description
        FROM iam.permissions p
        JOIN iam.role_permissions rp ON p.code = rp.permission_code
        WHERE rp.role = :role
        """, nativeQuery = true)
    List<Permission> findPermissionObjectsByRole(@Param("role") String role);

    @Query(value = """
        SELECT COUNT(*) > 0
        FROM iam.role_permissions
        WHERE role = :role AND permission_code = :permissionCode
        """, nativeQuery = true)
    boolean existsByRoleAndPermissionCode(@Param("role") String role, 
                                          @Param("permissionCode") String permissionCode);

    @Query(value = """
        SELECT rp.permission_code
        FROM iam.role_permissions rp
        WHERE rp.role = :role
        """, nativeQuery = true)
    List<String> findPermissionCodesByRole(@Param("role") String role);
}
