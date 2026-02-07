package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.Resident;
import com.QhomeBase.baseservice.model.ResidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResidentRepository extends JpaRepository<Resident, UUID> {

    List<Resident> findAllByStatus(ResidentStatus status);

    @Query("SELECT r FROM Resident r WHERE " +
            "(LOWER(r.fullName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(r.phone) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(r.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<Resident> searchByTerm(@Param("searchTerm") String searchTerm);

    @Query("SELECT r FROM Resident r WHERE r.phone IS NOT NULL AND r.phone LIKE CONCAT(:phonePrefix, '%') AND r.status = 'ACTIVE' ORDER BY r.fullName ASC")
    List<Resident> findByPhonePrefix(@Param("phonePrefix") String phonePrefix);

    Optional<Resident> findByPhone(String phone);

    Optional<Resident> findByEmail(String email);

    Optional<Resident> findByNationalId(String nationalId);

    boolean existsByPhone(String phone);

    boolean existsByEmail(String email);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Resident r WHERE LOWER(r.email) = LOWER(:email)")
    boolean existsByEmailIgnoreCase(@Param("email") String email);

    boolean existsByNationalId(String nationalId);

    boolean existsByPhoneAndIdNot(String phone, UUID id);

    boolean existsByEmailAndIdNot(String email, UUID id);

    boolean existsByNationalIdAndIdNot(String nationalId, UUID id);

    Optional<Resident> findByUserId(UUID userId);

    @Query(value = """
            SELECT DISTINCT r.user_id
            FROM data.residents r
            INNER JOIN data.household_members hm ON hm.resident_id = r.id
            INNER JOIN data.households h ON h.id = hm.household_id
            INNER JOIN data.units u ON u.id = h.unit_id
            WHERE u.building_id = :buildingId
            AND r.user_id IS NOT NULL
            AND (hm.left_at IS NULL OR hm.left_at > CURRENT_DATE)
            AND (h.end_date IS NULL OR h.end_date > CURRENT_DATE)
            """, nativeQuery = true)
    List<UUID> findUserIdsByBuildingId(@Param("buildingId") UUID buildingId);
}
