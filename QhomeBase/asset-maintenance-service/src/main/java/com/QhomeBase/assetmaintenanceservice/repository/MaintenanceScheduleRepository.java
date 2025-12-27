package com.QhomeBase.assetmaintenanceservice.repository;

import com.QhomeBase.assetmaintenanceservice.model.MaintenanceSchedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MaintenanceScheduleRepository extends JpaRepository<MaintenanceSchedule, UUID> {
    
    Optional<MaintenanceSchedule> findById(UUID id);
    
    @Query("SELECT COUNT(ms) > 0 FROM MaintenanceSchedule ms WHERE ms.asset.id = :assetId AND ms.name = :name AND ms.id != :excludeId")
    boolean existsByAssetIdAndName(@Param("assetId") UUID assetId, @Param("name") String name, @Param("excludeId") UUID excludeId);
    
    @Query("SELECT COUNT(ms) > 0 FROM MaintenanceSchedule ms WHERE ms.asset.id = :assetId AND ms.name = :name")
    boolean existsByAssetIdAndName(@Param("assetId") UUID assetId, @Param("name") String name);
    
    List<MaintenanceSchedule> findByAssetId(UUID assetId);
    
    List<MaintenanceSchedule> findByAssetIdAndIsActiveTrue(UUID assetId);
    
    List<MaintenanceSchedule> findByAssignedTo(UUID assignedTo);
    
    List<MaintenanceSchedule> findByAssignedToAndIsActiveTrue(UUID assignedTo);
    
    @Query("SELECT ms FROM MaintenanceSchedule ms WHERE ms.isActive = true AND ms.nextMaintenanceDate <= :endDate AND (:startDate IS NULL OR ms.nextMaintenanceDate >= :startDate)")
    List<MaintenanceSchedule> findUpcomingSchedules(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT ms FROM MaintenanceSchedule ms WHERE ms.asset.id = :assetId AND ms.isActive = true AND ms.nextMaintenanceDate <= :endDate AND (:startDate IS NULL OR ms.nextMaintenanceDate >= :startDate)")
    List<MaintenanceSchedule> findUpcomingSchedulesByAsset(@Param("assetId") UUID assetId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT ms FROM MaintenanceSchedule ms WHERE ms.assignedTo = :assignedTo AND ms.isActive = true AND ms.nextMaintenanceDate <= :endDate AND (:startDate IS NULL OR ms.nextMaintenanceDate >= :startDate)")
    List<MaintenanceSchedule> findUpcomingSchedulesByAssignedTo(@Param("assignedTo") UUID assignedTo, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    Page<MaintenanceSchedule> findByIsActiveTrue(Pageable pageable);
    
    Page<MaintenanceSchedule> findByAssetId(UUID assetId, Pageable pageable);
    
    Page<MaintenanceSchedule> findByAssignedTo(UUID assignedTo, Pageable pageable);
    
    @Query("SELECT ms FROM MaintenanceSchedule ms WHERE " +
           "(:assetId IS NULL OR ms.asset.id = :assetId) AND " +
           "(:assignedTo IS NULL OR ms.assignedTo = :assignedTo) AND " +
           "(:isActive IS NULL OR ms.isActive = :isActive) AND " +
           "(:maintenanceType IS NULL OR ms.maintenanceType = :maintenanceType)")
    List<MaintenanceSchedule> findWithFilters(@Param("assetId") UUID assetId,
                                              @Param("assignedTo") UUID assignedTo,
                                              @Param("isActive") Boolean isActive,
                                              @Param("maintenanceType") String maintenanceType)
                                              ;
}
