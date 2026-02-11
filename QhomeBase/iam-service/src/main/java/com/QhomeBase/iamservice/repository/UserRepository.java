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

    // Thêm vào sau các phương thức hiện có
    @Query(value = """
            SELECT u.*
            FROM iam.users u
            LEFT JOIN iam.staff_profiles sp ON u.id = sp.user_id
            LEFT JOIN data.residents r ON u.id = r.user_id
            WHERE u.username = :loginId
               OR u.email = :loginId
               OR sp.phone = :loginId
               OR r.phone = :loginId
            """, nativeQuery = true)
    Optional<User> findByLoginIdentifier(@Param("loginId") String loginId);

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

    @Query("""
                select distinct u
                from User u
                join fetch u.roles r
                where r in (
                    com.QhomeBase.iamservice.model.UserRole.ADMIN,
                    com.QhomeBase.iamservice.model.UserRole.ACCOUNTANT,
                    com.QhomeBase.iamservice.model.UserRole.TECHNICIAN,
                    com.QhomeBase.iamservice.model.UserRole.SUPPORTER
                )
            """)
    List<User> findStaffUsers();

}
