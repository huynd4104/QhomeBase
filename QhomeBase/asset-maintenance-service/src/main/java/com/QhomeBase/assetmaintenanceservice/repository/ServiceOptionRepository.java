package com.QhomeBase.assetmaintenanceservice.repository;

import com.QhomeBase.assetmaintenanceservice.model.service.ServiceOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ServiceOptionRepository extends JpaRepository<ServiceOption, UUID> {

    List<ServiceOption> findAllByServiceId(UUID serviceId);

    boolean existsByServiceIdAndCodeIgnoreCase(UUID serviceId, String code);

    boolean existsByServiceIdAndCodeIgnoreCaseAndIdNot(UUID serviceId, String code, UUID excludeId);
}

