package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.Meter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MeterRepository extends JpaRepository<Meter, UUID> {
    
    Optional<Meter> findByMeterCode(String meterCode);
    
    List<Meter> findByUnitId(UUID unitId);
    
    List<Meter> findByServiceId(UUID serviceId);
    
    @Query("SELECT m FROM Meter m WHERE m.unit.id = :unitId AND m.service.id = :serviceId AND m.active = true")
    Optional<Meter> findByUnitAndService(@Param("unitId") UUID unitId, @Param("serviceId") UUID serviceId);
    
    @Query("""
        SELECT m FROM Meter m
        JOIN m.unit u
        WHERE u.building.id = :buildingId
          AND m.service.id = :serviceId
          AND u.floor = :floor
          AND m.active = true
        ORDER BY u.floor, u.code
    """)
    List<Meter> findByBuildingServiceAndFloor(
        @Param("buildingId") UUID buildingId,
        @Param("serviceId") UUID serviceId,
        @Param("floor") Integer floor
    );
    
    @Query("""
        SELECT m FROM Meter m
        JOIN m.unit u
        WHERE u.building.id = :buildingId
          AND m.service.id = :serviceId
          AND m.active = true
        ORDER BY u.floor, u.code
    """)
    List<Meter> findByBuildingAndService(
        @Param("buildingId") UUID buildingId,
        @Param("serviceId") UUID serviceId
    );

    @Query("""
        SELECT m FROM Meter m
        JOIN m.unit u
        WHERE u.building.id = :buildingId
        ORDER BY u.floor, u.code, m.meterCode
    """)
    List<Meter> findByBuildingId(@Param("buildingId") UUID buildingId);

    @Query("SELECT m FROM Meter m WHERE m.active = :active ORDER BY m.meterCode")
    List<Meter> findByActive(@Param("active") Boolean active);
    
    @Query("""
        SELECT m FROM Meter m
        JOIN m.unit u
        WHERE u.id IN :unitIds
          AND m.service.id = :serviceId
          AND m.active = true
        ORDER BY u.floor, u.code, m.meterCode
    """)
    List<Meter> findByUnitIdsAndService(
        @Param("unitIds") List<UUID> unitIds,
        @Param("serviceId") UUID serviceId
    );

    @Query("SELECT DISTINCT m.unit.id FROM Meter m WHERE m.service.id = :serviceId AND m.active = true")
    List<UUID> findUnitIdsByServiceId(@Param("serviceId") UUID serviceId);
}

