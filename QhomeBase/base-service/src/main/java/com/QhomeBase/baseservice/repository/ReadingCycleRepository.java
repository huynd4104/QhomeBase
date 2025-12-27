package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.ReadingCycle;
import com.QhomeBase.baseservice.model.ReadingCycleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReadingCycleRepository extends JpaRepository<ReadingCycle, UUID> {
    
    List<ReadingCycle> findByStatus(ReadingCycleStatus status);
    
    List<ReadingCycle> findByStatusIn(List<ReadingCycleStatus> statuses);
    
    Optional<ReadingCycle> findByName(String name);
    
    @Query("SELECT rc FROM ReadingCycle rc WHERE rc.name = :name AND rc.service.id = :serviceId")
    Optional<ReadingCycle> findByNameAndServiceId(@Param("name") String name, @Param("serviceId") UUID serviceId);
    
    @Query("SELECT rc FROM ReadingCycle rc WHERE rc.service.id = :serviceId")
    List<ReadingCycle> findByServiceId(@Param("serviceId") UUID serviceId);
    
    @Query("SELECT rc FROM ReadingCycle rc WHERE rc.status = :status AND rc.service.id = :serviceId")
    List<ReadingCycle> findByStatusAndServiceId(@Param("status") ReadingCycleStatus status, @Param("serviceId") UUID serviceId);
    
    @Query("SELECT rc FROM ReadingCycle rc WHERE rc.periodFrom <= :endDate AND rc.periodTo >= :startDate AND rc.service.id = :serviceId")
    List<ReadingCycle> findOverlappingCyclesByService(@Param("startDate") LocalDate startDate, 
                                                       @Param("endDate") LocalDate endDate,
                                                       @Param("serviceId") UUID serviceId);
    
    @Query("SELECT rc FROM ReadingCycle rc WHERE rc.periodFrom <= :endDate AND rc.periodTo >= :startDate")
    List<ReadingCycle> findOverlappingCycles(@Param("startDate") LocalDate startDate, 
                                              @Param("endDate") LocalDate endDate);
    
    @Query("SELECT rc FROM ReadingCycle rc WHERE rc.status = 'ACTIVE' AND rc.periodTo < :date")
    List<ReadingCycle> findActiveExpiredCycles(@Param("date") LocalDate date);
}
