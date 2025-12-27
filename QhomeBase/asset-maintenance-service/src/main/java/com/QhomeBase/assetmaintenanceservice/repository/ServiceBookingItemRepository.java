package com.QhomeBase.assetmaintenanceservice.repository;

import com.QhomeBase.assetmaintenanceservice.model.service.ServiceBookingItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceBookingItemRepository extends JpaRepository<ServiceBookingItem, UUID> {

    List<ServiceBookingItem> findAllByBookingId(UUID bookingId);

    Optional<ServiceBookingItem> findByIdAndBookingId(UUID id, UUID bookingId);
}







