package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.Household;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HouseholdRepository extends JpaRepository<Household, UUID> {

    // Optimized query - uses composite index on (unit_id, end_date) for fast lookup
    // Index: idx_households_unit_end_date
    @Query("SELECT h FROM Household h " +
            "WHERE h.unitId = :unitId AND (h.endDate IS NULL OR h.endDate >= CURRENT_DATE) " +
            "ORDER BY h.startDate DESC")
    Optional<Household> findCurrentHouseholdByUnitId(@Param("unitId") UUID unitId);
    
    // Batch query to get all current households for multiple units at once
    // This eliminates N+1 query problem when loading units with their households
    @Query("SELECT h FROM Household h " +
            "WHERE h.unitId IN :unitIds AND (h.endDate IS NULL OR h.endDate >= CURRENT_DATE) " +
            "ORDER BY h.unitId, h.startDate DESC")
    List<Household> findCurrentHouseholdsByUnitIds(@Param("unitIds") List<UUID> unitIds);
}




