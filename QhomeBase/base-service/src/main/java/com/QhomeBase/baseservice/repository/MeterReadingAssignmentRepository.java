package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.MeterReadingAssignment;
import com.QhomeBase.baseservice.model.MeterReadingAssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface MeterReadingAssignmentRepository extends JpaRepository<MeterReadingAssignment, UUID> {
    
    List<MeterReadingAssignment> findByCycleId(UUID cycleId);
    
    List<MeterReadingAssignment> findByAssignedTo(UUID assignedTo);
    
    List<MeterReadingAssignment> findByBuildingId(UUID buildingId);
    
    List<MeterReadingAssignment> findByServiceId(UUID serviceId);
    
    List<MeterReadingAssignment> findByCycleIdAndCompletedAtIsNull(UUID cycleId);
    
    List<MeterReadingAssignment> findByAssignedToAndCompletedAtIsNull(UUID assignedTo);
    
    List<MeterReadingAssignment> findByBuildingIdAndServiceId(UUID buildingId, UUID serviceId);
    
    List<MeterReadingAssignment> findByBuildingIdAndServiceIdAndCompletedAtIsNull(UUID buildingId, UUID serviceId);
    
    @Query("SELECT a FROM MeterReadingAssignment a WHERE a.assignedTo = :staffId " +
           "AND a.status NOT IN :excludedStatuses")
    List<MeterReadingAssignment> findActiveByAssignedTo(
        @Param("staffId") UUID staffId,
        @Param("excludedStatuses") List<MeterReadingAssignmentStatus> excludedStatuses
    );
    
    List<MeterReadingAssignment> findByAssignedToAndStatusIn(
        UUID assignedTo,
        List<MeterReadingAssignmentStatus> statuses
    );

    @Query("SELECT a FROM MeterReadingAssignment a WHERE a.assignedTo = :staffId AND a.cycle.id = :cycleId")
    List<MeterReadingAssignment> findByAssignedToAndCycleId(
        @Param("staffId") UUID staffId,
        @Param("cycleId") UUID cycleId
    );

    @Query("""
            SELECT DISTINCT a FROM MeterReadingAssignment a
            LEFT JOIN FETCH a.cycle c
            LEFT JOIN FETCH a.building b
            WHERE a.status IN :statuses
              AND a.endDate IS NOT NULL
              AND a.endDate BETWEEN :from AND :to
              AND (a.reminderLastSentDate IS NULL OR a.reminderLastSentDate < :today)
            """)
    List<MeterReadingAssignment> findAssignmentsNeedingReminder(
            @Param("statuses") List<MeterReadingAssignmentStatus> statuses,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("today") LocalDate today
    );

}
