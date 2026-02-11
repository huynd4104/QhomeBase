package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.Household;
import com.QhomeBase.baseservice.model.HouseholdKind;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDate;
import com.QhomeBase.baseservice.dto.residentview.*;

@Repository
public interface HouseholdRepository extends JpaRepository<Household, UUID> {

        // Optimized query - uses composite index on (unit_id, end_date) for fast lookup
        // Index: idx_households_unit_end_date
        @Query("SELECT h FROM Household h " +
                        "WHERE h.unitId = :unitId AND (h.endDate IS NULL OR h.endDate >= CURRENT_DATE) " +
                        "ORDER BY h.startDate DESC")
        Optional<Household> findCurrentHouseholdByUnitId(@Param("unitId") UUID unitId);

        @Query("SELECT h FROM Household h WHERE h.unitId = :unitId AND (h.endDate IS NULL OR h.endDate >= CURRENT_DATE)")
        List<Household> findCurrentByUnitId(@Param("unitId") UUID unitId);

        List<Household> findByUnitIdAndKind(UUID unitId, HouseholdKind kind);

        // Batch query to get all current households for multiple units at once
        // This eliminates N+1 query problem when loading units with their households
        @Query("SELECT h FROM Household h " +
                        "WHERE h.unitId IN :unitIds AND (h.endDate IS NULL OR h.endDate >= CURRENT_DATE) " +
                        "ORDER BY h.unitId, h.startDate DESC")
        List<Household> findCurrentHouseholdsByUnitIds(@Param("unitIds") List<UUID> unitIds);

        @Query("SELECT min(h.startDate) FROM Household h")
        LocalDate findMinStartDate();

        @Query("SELECT max(h.endDate) FROM Household h")
        LocalDate findMaxEndDate();

        @Query("SELECT new com.QhomeBase.baseservice.dto.residentview.ResidentViewBuildingDto(" +
                        "b.id, b.code, b.name, " +
                        "count(distinct rm.residentId), " +
                        "count(distinct h.unitId)) " +
                        "FROM Household h " +
                        "JOIN Unit u ON h.unitId = u.id " +
                        "JOIN Building b ON u.building.id = b.id " +
                        "JOIN HouseholdMember rm ON rm.householdId = h.id " +
                        "WHERE h.startDate <= :yearEnd AND (h.endDate IS NULL OR h.endDate >= :yearStart) " +
                        "AND (rm.joinedAt <= :yearEnd AND (rm.leftAt IS NULL OR rm.leftAt >= :yearStart)) " +
                        "GROUP BY b.id, b.code, b.name")
        List<ResidentViewBuildingDto> findBuildingsByYear(@Param("yearStart") LocalDate yearStart,
                        @Param("yearEnd") LocalDate yearEnd);

        @Query("SELECT new com.QhomeBase.baseservice.dto.residentview.ResidentViewFloorDto(" +
                        "u.floor, " +
                        "count(distinct u.id), " +
                        "count(distinct h.unitId)) " +
                        "FROM Household h " +
                        "JOIN Unit u ON h.unitId = u.id " +
                        "JOIN HouseholdMember rm ON rm.householdId = h.id " +
                        "WHERE u.building.id = :buildingId " +
                        "AND h.startDate <= :yearEnd AND (h.endDate IS NULL OR h.endDate >= :yearStart) " +
                        "AND (rm.joinedAt <= :yearEnd AND (rm.leftAt IS NULL OR rm.leftAt >= :yearStart)) " +
                        "GROUP BY u.floor")
        List<ResidentViewFloorDto> findFloorsByYearAndBuilding(@Param("yearStart") LocalDate yearStart,
                        @Param("yearEnd") LocalDate yearEnd, @Param("buildingId") UUID buildingId);

        @Query("SELECT new com.QhomeBase.baseservice.dto.residentview.ResidentViewUnitDto(" +
                        "u.id, u.code, " +
                        "count(distinct rm.residentId)) " +
                        "FROM Household h " +
                        "JOIN Unit u ON h.unitId = u.id " +
                        "JOIN HouseholdMember rm ON rm.householdId = h.id " +
                        "WHERE u.building.id = :buildingId AND u.floor = :floor " +
                        "AND h.startDate <= :yearEnd AND (h.endDate IS NULL OR h.endDate >= :yearStart) " +
                        "AND (rm.joinedAt <= :yearEnd AND (rm.leftAt IS NULL OR rm.leftAt >= :yearStart)) " +
                        "GROUP BY u.id, u.code")
        List<ResidentViewUnitDto> findUnitsByYearBuildingAndFloor(@Param("yearStart") LocalDate yearStart,
                        @Param("yearEnd") LocalDate yearEnd, @Param("buildingId") UUID buildingId,
                        @Param("floor") Integer floor);

        @Query("SELECT new com.QhomeBase.baseservice.dto.residentview.ResidentViewResidentDto(" +
                        "r.id, r.fullName, r.phone, r.email, r.nationalId, r.dob, " +
                        "rm.relation, cast(h.kind as string), rm.isPrimary, cast(r.status as string)) " +
                        "FROM Household h " +
                        "JOIN HouseholdMember rm ON rm.householdId = h.id " +
                        "JOIN Resident r ON rm.residentId = r.id " +
                        "WHERE h.unitId = :unitId " +
                        "AND h.startDate <= :yearEnd AND (h.endDate IS NULL OR h.endDate >= :yearStart) " +
                        "AND (rm.joinedAt <= :yearEnd AND (rm.leftAt IS NULL OR rm.leftAt >= :yearStart))")
        List<ResidentViewResidentDto> findResidentsByUnitAndYear(@Param("yearStart") LocalDate yearStart,
                        @Param("yearEnd") LocalDate yearEnd, @Param("unitId") UUID unitId);

        @Query("SELECT new com.QhomeBase.baseservice.dto.residentview.ResidentExportDto(" +
                        ":year, " +
                        "b.code, u.code, " +
                        "r.fullName, r.phone, r.email, r.nationalId, r.dob, cast(r.status as string), " +
                        "cast(h.kind as string), " +
                        "rm.relation, rm.isPrimary, " +
                        "h.startDate, rm.joinedAt) " +
                        "FROM Household h " +
                        "JOIN Unit u ON h.unitId = u.id " +
                        "JOIN Building b ON u.building.id = b.id " +
                        "JOIN HouseholdMember rm ON rm.householdId = h.id " +
                        "JOIN Resident r ON rm.residentId = r.id " +
                        "WHERE h.startDate <= :yearEnd AND (h.endDate IS NULL OR h.endDate >= :yearStart) " +
                        "AND (rm.joinedAt <= :yearEnd AND (rm.leftAt IS NULL OR rm.leftAt >= :yearStart)) " +
                        "AND (:buildingId IS NULL OR b.id = :buildingId) " +
                        "AND (:floor IS NULL OR u.floor = :floor) " +
                        "ORDER BY b.code, u.floor, u.code, r.fullName")
        List<ResidentExportDto> findResidentsForExport(
                        @Param("year") Integer year,
                        @Param("yearStart") LocalDate yearStart,
                        @Param("yearEnd") LocalDate yearEnd,
                        @Param("buildingId") UUID buildingId,
                        @Param("floor") Integer floor);

}
