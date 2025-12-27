package com.QhomeBase.iamservice.repository;

import com.QhomeBase.iamservice.model.Roles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RolesRepository extends JpaRepository<Roles, String> {
    
    @Query(value = """
        SELECT r.role, r.description, r.created_at
        FROM iam.roles r
        WHERE r.role IN ('ADMIN', 'TECHNICIAN', 'SUPPORTER', 'ACCOUNTANT', 'RESIDENT', 'UNIT_OWNER')
        ORDER BY r.role
        """, nativeQuery = true)
    List<Object[]> findGlobalRoles();
    
    @Query(value = """
        SELECT rp.permission_code
        FROM iam.role_permissions rp
        WHERE rp.role = :roleName
        ORDER BY rp.permission_code
        """, nativeQuery = true)
    List<String> findPermissionsByRole(@Param("roleName") String roleName);
    
    @Query(value = """
        SELECT r.role, r.description
        FROM iam.roles r
        WHERE r.role = :roleName
        """, nativeQuery = true)
    Object[] findRoleByName(@Param("roleName") String roleName);
    
    @Query(value = """
        SELECT COUNT(*) > 0
        FROM iam.roles r
        WHERE r.role = :roleName
        AND r.role IN ('ADMIN', 'TECHNICIAN', 'SUPPORTER', 'ACCOUNTANT', 'RESIDENT', 'UNIT_OWNER')
        """, nativeQuery = true)
    boolean isValidRole(@Param("roleName") String roleName);
}
