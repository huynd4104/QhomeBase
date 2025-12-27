package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.MeterReadingReminder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MeterReadingReminderRepository extends JpaRepository<MeterReadingReminder, UUID> {

    List<MeterReadingReminder> findByUserIdAndAcknowledgedAtIsNullOrderByCreatedAtDesc(UUID userId);

    List<MeterReadingReminder> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<MeterReadingReminder> findByIdAndUserId(UUID id, UUID userId);

    List<MeterReadingReminder> findByAssignment_Id(UUID assignmentId);
}

