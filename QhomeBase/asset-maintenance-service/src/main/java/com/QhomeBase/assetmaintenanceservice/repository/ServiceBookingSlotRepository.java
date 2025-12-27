package com.QhomeBase.assetmaintenanceservice.repository;

import com.QhomeBase.assetmaintenanceservice.model.service.ServiceBookingSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public interface ServiceBookingSlotRepository extends JpaRepository<ServiceBookingSlot, UUID> {

    List<ServiceBookingSlot> findAllByBookingId(UUID bookingId);

    boolean existsByServiceIdAndSlotDateAndStartTimeLessThanAndEndTimeGreaterThan(
            UUID serviceId,
            LocalDate slotDate,
            LocalTime endTime,
            LocalTime startTime
    );

    boolean existsByServiceIdAndSlotDateAndStartTimeLessThanAndEndTimeGreaterThanAndBooking_IdNot(
            UUID serviceId,
            LocalDate slotDate,
            LocalTime endTime,
            LocalTime startTime,
            UUID excludeBookingId
    );

    List<ServiceBookingSlot> findAllByServiceIdAndSlotDateBetweenOrderBySlotDateAscStartTimeAsc(
            UUID serviceId,
            LocalDate startDate,
            LocalDate endDate
    );

    List<ServiceBookingSlot> findAllByServiceIdOrderBySlotDateAscStartTimeAsc(UUID serviceId);

    List<ServiceBookingSlot> findAllByServiceIdAndSlotDateOrderByStartTimeAsc(UUID serviceId, LocalDate slotDate);

    List<ServiceBookingSlot> findAllByServiceIdAndSlotDateAndStartTimeLessThanAndEndTimeGreaterThan(
            UUID serviceId,
            LocalDate slotDate,
            LocalTime endTime,
            LocalTime startTime
    );

    List<ServiceBookingSlot> findAllByServiceIdAndSlotDateAndStartTimeLessThanAndEndTimeGreaterThanAndBooking_IdNot(
            UUID serviceId,
            LocalDate slotDate,
            LocalTime endTime,
            LocalTime startTime,
            UUID excludeBookingId
    );
}


