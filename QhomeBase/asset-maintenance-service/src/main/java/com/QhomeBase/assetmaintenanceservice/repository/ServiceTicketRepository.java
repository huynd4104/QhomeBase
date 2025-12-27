package com.QhomeBase.assetmaintenanceservice.repository;

import com.QhomeBase.assetmaintenanceservice.model.service.ServiceTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ServiceTicketRepository extends JpaRepository<ServiceTicket, UUID> {

    List<ServiceTicket> findAllByServiceId(UUID serviceId);

    boolean existsByServiceIdAndCodeIgnoreCase(UUID serviceId, String code);

    boolean existsByServiceIdAndCodeIgnoreCaseAndIdNot(UUID serviceId, String code, UUID ticketId);
}
