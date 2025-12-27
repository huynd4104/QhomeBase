package com.QhomeBase.iamservice.repository;

import com.QhomeBase.iamservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);

    @Query(value = """
        SELECT DISTINCT u.* 
        FROM iam.users u
        WHERE EXISTS (
            SELECT 1 FROM iam.user_roles ur 
            WHERE ur.user_id = u.id 
            AND ur.role IN ('TECHNICIAN', 'SUPPORTER', 'ACCOUNTANT')
        )
        AND u.active = true
        """, nativeQuery = true)
    List<User> findAvailableStaff();

    @Query(value = """
        SELECT DISTINCT u.* 
        FROM iam.users u
        WHERE EXISTS (
            SELECT 1 FROM iam.user_roles ur 
            WHERE ur.user_id = u.id 
            AND ur.role = :role
        )
        AND u.active = true
        """, nativeQuery = true)
    List<User> findByRole(@Param("role") String role);

    @Query(value = """
        SELECT DISTINCT u.* 
        FROM iam.users u
        JOIN iam.user_roles ur ON u.id = ur.user_id
        WHERE ur.role = :role
        """, nativeQuery = true)
    List<User> findByRoleIncludingInactive(@Param("role") String role);

    @Query(value = """
        SELECT DISTINCT u.*
        FROM iam.users u
        WHERE EXISTS (
            SELECT 1 FROM iam.user_roles ur
            WHERE ur.user_id = u.id
            AND ur.role IN ('ADMIN', 'ACCOUNTANT', 'TECHNICIAN', 'SUPPORTER')
        )
        """, nativeQuery = true)
    List<User> findStaffUsers();
}
