package com.QhomeBase.assetmaintenanceservice.repository;

import com.QhomeBase.assetmaintenanceservice.model.service.ServiceCombo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceComboRepository extends JpaRepository<ServiceCombo, UUID> {

    List<ServiceCombo> findAllByServiceId(UUID serviceId);

    boolean existsByServiceIdAndCodeIgnoreCase(UUID serviceId, String code);

    Optional<ServiceCombo> findByIdAndServiceId(UUID comboId, UUID serviceId);
}

