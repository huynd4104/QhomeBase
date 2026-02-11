package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.HouseholdMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HouseholdMemberRepository extends JpaRepository<HouseholdMember, UUID> {

       @Query("SELECT hm FROM HouseholdMember hm " +
                     "WHERE hm.householdId = :householdId " +
                     "AND (hm.leftAt IS NULL OR hm.leftAt >= CURRENT_DATE) " +
                     "ORDER BY hm.isPrimary DESC, hm.joinedAt ASC")
       List<HouseholdMember> findActiveMembersByHouseholdId(@Param("householdId") UUID householdId);

       @Query(value = "SELECT hm.* FROM data.household_members hm " +
                     "WHERE hm.household_id = :householdId " +
                     "AND (hm.left_at IS NULL OR hm.left_at >= CURRENT_DATE) " +
                     "ORDER BY hm.is_primary DESC, hm.joined_at ASC " +
                     "LIMIT :limit OFFSET :offset", nativeQuery = true)
       List<HouseholdMember> findActiveMembersByHouseholdIdWithPagination(
                     @Param("householdId") UUID householdId,
                     @Param("limit") int limit,
                     @Param("offset") int offset);

       @Query("SELECT hm FROM HouseholdMember hm " +
                     "WHERE hm.householdId = :householdId " +
                     "AND hm.isPrimary = true " +
                     "AND (hm.leftAt IS NULL OR hm.leftAt >= CURRENT_DATE)")
       Optional<HouseholdMember> findPrimaryMemberByHouseholdId(@Param("householdId") UUID householdId);

       @Query("SELECT hm FROM HouseholdMember hm " +
                     "WHERE hm.residentId = :residentId " +
                     "AND (hm.leftAt IS NULL OR hm.leftAt >= CURRENT_DATE)")
       List<HouseholdMember> findActiveMembersByResidentId(@Param("residentId") UUID residentId);

       @Query("SELECT hm FROM HouseholdMember hm " +
                     "JOIN Household h ON hm.householdId = h.id " +
                     "WHERE hm.residentId = :residentId " +
                     "AND h.unitId = :unitId " +
                     "AND (hm.leftAt IS NULL OR hm.leftAt >= CURRENT_DATE)")
       Optional<HouseholdMember> findMemberByResidentAndUnit(
                     @Param("residentId") UUID residentId,
                     @Param("unitId") UUID unitId);

       @Query("SELECT hm FROM HouseholdMember hm " +
                     "WHERE hm.householdId = :householdId " +
                     "AND hm.residentId = :residentId " +
                     "AND (hm.leftAt IS NULL OR hm.leftAt >= CURRENT_DATE)")
       Optional<HouseholdMember> findActiveMemberByResidentAndHousehold(
                     @Param("residentId") UUID residentId,
                     @Param("householdId") UUID householdId);

       @Query("SELECT COUNT(hm) FROM HouseholdMember hm " +
                     "WHERE hm.householdId = :householdId " +
                     "AND (hm.leftAt IS NULL OR hm.leftAt >= CURRENT_DATE)")
       long countActiveMembersByHouseholdId(@Param("householdId") UUID householdId);

       @Query("SELECT COUNT(hm) FROM HouseholdMember hm " +
                     "JOIN Resident r ON hm.residentId = r.id " +
                     "WHERE hm.householdId = :householdId " +
                     "AND (hm.leftAt IS NULL OR hm.leftAt >= CURRENT_DATE) " +
                     "AND r.userId IS NOT NULL")
       long countActiveMembersWithAccount(@Param("householdId") UUID householdId);

       @Query("SELECT hm FROM HouseholdMember hm " +
                     "WHERE hm.householdId = :householdId " +
                     "ORDER BY hm.joinedAt ASC, hm.isPrimary DESC")
       List<HouseholdMember> findAllMembersByHouseholdId(@Param("householdId") UUID householdId);

       boolean existsByHouseholdIdAndResidentId(UUID householdId, UUID residentId);

}
