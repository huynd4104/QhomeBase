package com.QhomeBase.servicescardservice.repository;

import com.QhomeBase.servicescardservice.model.CardFeeReminderState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CardFeeReminderStateRepository extends JpaRepository<CardFeeReminderState, UUID> {

    Optional<CardFeeReminderState> findByCardTypeAndCardId(String cardType, UUID cardId);

    @Query("""
        SELECT s FROM CardFeeReminderState s
        WHERE s.nextDueDate <= :today
          AND s.nextDueDate >= :cutoffDate
          AND s.reminderCount < s.maxReminders
        ORDER BY s.unitId, s.nextDueDate ASC
        """)
    List<CardFeeReminderState> findDueStates(@Param("today") LocalDate today, 
                                              @Param("cutoffDate") LocalDate cutoffDate);

    @Query("SELECT s FROM CardFeeReminderState s WHERE s.unitId = :unitId")
    List<CardFeeReminderState> findByUnitId(@Param("unitId") UUID unitId);

    @Query("SELECT s FROM CardFeeReminderState s WHERE s.residentId = :residentId")
    List<CardFeeReminderState> findByResidentId(@Param("residentId") UUID residentId);
}

