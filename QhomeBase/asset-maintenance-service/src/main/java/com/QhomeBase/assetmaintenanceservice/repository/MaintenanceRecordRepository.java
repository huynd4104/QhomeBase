package com.QhomeBase.assetmaintenanceservice.repository;

import com.QhomeBase.assetmaintenanceservice.model.MaintenanceRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, UUID> {

    Optional<MaintenanceRecord> findById(UUID id);

    List<MaintenanceRecord> findByAssetId(UUID assetId);

    List<MaintenanceRecord> findByAssignedTo(UUID assignedTo);

    @Query("SELECT COUNT(mr) FROM MaintenanceRecord mr WHERE mr.assignedTo = :technicianId AND mr.status = :status")
    long countByAssignedToAndStatus(@Param("technicianId") UUID technicianId, @Param("status") String status);

    @Query("SELECT COUNT(mr) FROM MaintenanceRecord mr WHERE mr.assignedTo = :technicianId AND mr.status IN ('ASSIGNED', 'IN_PROGRESS')")
    long countPendingTasksByAssignedTo(@Param("technicianId") UUID technicianId);

    @Query("SELECT COUNT(mr) FROM MaintenanceRecord mr WHERE mr.assignedTo = :technicianId AND mr.status = 'ASSIGNED'")
    long countAssignedTasksByAssignedTo(@Param("technicianId") UUID technicianId);

    @Query("SELECT COUNT(mr) FROM MaintenanceRecord mr WHERE mr.assignedTo = :technicianId AND mr.status = 'IN_PROGRESS'")
    long countInProgressTasksByAssignedTo(@Param("technicianId") UUID technicianId);

    @Query("SELECT mr FROM MaintenanceRecord mr WHERE " +
           "(:assetId IS NULL OR mr.asset.id = :assetId) AND " +
           "(:maintenanceType IS NULL OR mr.maintenanceType = :maintenanceType) AND " +
           "(:status IS NULL OR mr.status = :status) AND " +
           "(:assignedTo IS NULL OR mr.assignedTo = :assignedTo) AND " +
           "(:maintenanceDateFrom IS NULL OR mr.maintenanceDate >= :maintenanceDateFrom) AND " +
           "(:maintenanceDateTo IS NULL OR mr.maintenanceDate <= :maintenanceDateTo) AND " +
           "(:scheduleId IS NULL OR mr.maintenanceSchedule.id = :scheduleId)")
    Page<MaintenanceRecord> findWithFilters(@Param("assetId") UUID assetId,
                                            @Param("maintenanceType") String maintenanceType,
                                            @Param("status") String status,
                                            @Param("assignedTo") UUID assignedTo,
                                            @Param("maintenanceDateFrom") LocalDate maintenanceDateFrom,
                                            @Param("maintenanceDateTo") LocalDate maintenanceDateTo,
                                            @Param("scheduleId") UUID scheduleId,
                                            Pageable pageable);

    @Query("SELECT mr FROM MaintenanceRecord mr WHERE mr.assignedTo = :technicianId AND mr.status IN (:statuses)")
    Page<MaintenanceRecord> findByAssignedToAndStatusIn(@Param("technicianId") UUID technicianId,
                                                         @Param("statuses") List<String> statuses,
                                                         Pageable pageable);

    @Query("SELECT mr FROM MaintenanceRecord mr WHERE mr.assignedTo = :technicianId")
    Page<MaintenanceRecord> findByAssignedTo(@Param("technicianId") UUID technicianId, Pageable pageable);

    @Query("SELECT DISTINCT mr.assignedTo FROM MaintenanceRecord mr WHERE mr.status IN ('ASSIGNED', 'IN_PROGRESS')")
    List<UUID> findTechniciansWithPendingTasks();

    @Query("SELECT mr FROM MaintenanceRecord mr WHERE mr.maintenanceSchedule.id = :scheduleId")
    List<MaintenanceRecord> findByMaintenanceScheduleId(@Param("scheduleId") UUID scheduleId);

    @Query("SELECT mr FROM MaintenanceRecord mr WHERE mr.asset.id = :assetId AND mr.status IN (:statuses)")
    List<MaintenanceRecord> findByAssetIdAndStatusIn(@Param("assetId") UUID assetId, @Param("statuses") List<String> statuses);

    @Query(value = """
        SELECT 
            mr.assigned_to as technicianId,
            COUNT(CASE WHEN mr.status IN ('ASSIGNED', 'IN_PROGRESS') THEN 1 END) as pendingCount,
            COUNT(CASE WHEN mr.status = 'ASSIGNED' THEN 1 END) as assignedCount,
            COUNT(CASE WHEN mr.status = 'IN_PROGRESS' THEN 1 END) as inProgressCount
        FROM asset.maintenance_records mr
        WHERE mr.status IN ('ASSIGNED', 'IN_PROGRESS')
          AND mr.assigned_to IS NOT NULL
        GROUP BY mr.assigned_to
    """, nativeQuery = true)
    List<Object[]> countWorkloadByTechnician();

    @Query(value = """
        SELECT 
            mr.assigned_to as technicianId,
            COUNT(CASE WHEN mr.status IN ('ASSIGNED', 'IN_PROGRESS') THEN 1 END) as pendingCount,
            COUNT(CASE WHEN mr.status = 'ASSIGNED' THEN 1 END) as assignedCount,
            COUNT(CASE WHEN mr.status = 'IN_PROGRESS' THEN 1 END) as inProgressCount
        FROM asset.maintenance_records mr
        WHERE mr.status IN ('ASSIGNED', 'IN_PROGRESS')
          AND mr.assigned_to IN (:technicianIds)
        GROUP BY mr.assigned_to
    """, nativeQuery = true)
    List<Object[]> countWorkloadByTechnicians(@Param("technicianIds") List<UUID> technicianIds);

    @Query(value = """
        SELECT 
            COUNT(CASE WHEN mr.status IN ('ASSIGNED', 'IN_PROGRESS') THEN 1 END) as pendingCount,
            COUNT(CASE WHEN mr.status = 'ASSIGNED' THEN 1 END) as assignedCount,
            COUNT(CASE WHEN mr.status = 'IN_PROGRESS' THEN 1 END) as inProgressCount
        FROM asset.maintenance_records mr
        WHERE mr.assigned_to = CAST(:technicianId AS UUID)
        AND mr.status IN ('ASSIGNED', 'IN_PROGRESS')
    """, nativeQuery = true)
    Object[] countWorkloadByTechnicianId(@Param("technicianId") UUID technicianId);
}


