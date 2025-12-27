package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {
    
    List<Vehicle> findAllByActive(Boolean active);
    
    @Query("SELECT v FROM Vehicle v JOIN FETCH v.unit")
    List<Vehicle> findAllWithUnit();
    
    @Query("SELECT v FROM Vehicle v WHERE v.residentId = :residentId")
    List<Vehicle> findAllByResidentId(@Param("residentId") UUID residentId);
    
    @Query("SELECT v FROM Vehicle v JOIN FETCH v.unit WHERE v.unit.id = :unitId")
    List<Vehicle> findAllByUnitId(@Param("unitId") UUID unitId);
    
    @Query("SELECT v FROM Vehicle v JOIN FETCH v.unit WHERE v.id = :id")
    Vehicle findByIdWithUnit(@Param("id") UUID id);
    
    boolean existsByPlateNo(String plateNo);
    
    boolean existsByPlateNoAndIdNot(String plateNo, UUID id);
    
    boolean existsByResidentId(UUID residentId);
    
    List<Vehicle> findAllByActiveTrue();
    
    // ========== New queries for activation tracking ==========
    
    /**
     * Find all activated vehicles (vehicles with activatedAt set)
     */
    List<Vehicle> findAllByActivatedAtIsNotNull();
    
    /**
     * Find all active and activated vehicles
     */
    @Query("SELECT v FROM Vehicle v WHERE v.active = true AND v.activatedAt IS NOT NULL")
    List<Vehicle> findAllActiveAndActivated();
    
    /**
     * Find vehicles activated in a specific month
     * Used for generating pro-rata invoices
     */
    @Query("""
        SELECT v FROM Vehicle v
        WHERE v.activatedAt IS NOT NULL
          AND v.active = true
          AND FUNCTION('DATE_TRUNC', 'month', v.activatedAt) = FUNCTION('DATE_TRUNC', 'month', :monthStart)
        ORDER BY v.activatedAt ASC
        """)
    List<Vehicle> findActivatedInMonth(@Param("monthStart") OffsetDateTime monthStart);
    
    /**
     * Find vehicles activated in a date range
     */
    @Query("""
        SELECT v FROM Vehicle v
        WHERE v.activatedAt BETWEEN :startDate AND :endDate
          AND v.active = true
        ORDER BY v.activatedAt ASC
        """)
    List<Vehicle> findActivatedBetween(@Param("startDate") OffsetDateTime startDate,
                                       @Param("endDate") OffsetDateTime endDate);
    
    /**
     * Find all pending activation vehicles (created but not yet activated)
     */
    @Query("SELECT v FROM Vehicle v WHERE v.activatedAt IS NULL")
    List<Vehicle> findAllPendingActivation();
    
    /**
     * Count activated vehicles
     */
    @Query("SELECT COUNT(v) FROM Vehicle v WHERE v.activatedAt IS NOT NULL")
    Long countActivated();
}
