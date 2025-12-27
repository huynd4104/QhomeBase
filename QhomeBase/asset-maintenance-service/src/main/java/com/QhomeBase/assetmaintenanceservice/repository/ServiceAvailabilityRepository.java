package com.QhomeBase.assetmaintenanceservice.repository;

import com.QhomeBase.assetmaintenanceservice.model.service.ServiceAvailability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ServiceAvailabilityRepository extends JpaRepository<ServiceAvailability, UUID> {

    List<ServiceAvailability> findByServiceIdAndDayOfWeekAndIsAvailableTrueOrderByStartTimeAsc(UUID serviceId, Integer dayOfWeek);
}
