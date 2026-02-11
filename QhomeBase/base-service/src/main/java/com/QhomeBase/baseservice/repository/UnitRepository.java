package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.Unit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UnitRepository extends JpaRepository<Unit, UUID> {
    @Query("SELECT u FROM Unit u JOIN FETCH u.building WHERE u.building.id = :buildingId")
    List<Unit> findAllByBuildingId(@Param("buildingId") UUID buildingId);

    @Query("SELECT u FROM Unit u JOIN FETCH u.building WHERE u.building.id = :buildingId AND u.floor = :floorNumber")
    List<Unit> findByBuildingIdAndFloorNumber(@Param("buildingId") UUID buildingId, @Param("floorNumber") int floorNumber);

    @Query("SELECT COUNT(u) FROM Unit u WHERE u.building.id = :buildingId AND u.floor = :floorNumber")
    long countByBuildingIdAndFloorNumber(@Param("buildingId") UUID buildingId, @Param("floorNumber") Integer floorNumber);

    @Query("SELECT u FROM Unit u JOIN FETCH u.building WHERE u.id = :id")
    Unit findByIdWithBuilding(@Param("id") UUID id);

    @Query("SELECT DISTINCT u FROM Unit u " +
           "JOIN FETCH u.building " +
           "JOIN Household h ON h.unitId = u.id " +
           "JOIN HouseholdMember hm ON hm.householdId = h.id " +
           "JOIN Resident r ON r.id = hm.residentId " +
           "WHERE r.userId = :userId " +
           "AND (hm.leftAt IS NULL OR hm.leftAt >= CURRENT_DATE) " +
           "AND (h.endDate IS NULL OR h.endDate >= CURRENT_DATE)")
    List<Unit> findAllUnitsByUserId(@Param("userId") UUID userId);

    @Query("SELECT u FROM Unit u JOIN FETCH u.building")
    List<Unit> findAllWithBuilding();

    @Query("SELECT u FROM Unit u JOIN FETCH u.building WHERE u.building.id = :buildingId AND u.code = :code")
    Optional<Unit> findByBuildingIdAndCode(@Param("buildingId") UUID buildingId, @Param("code") String code);



}
