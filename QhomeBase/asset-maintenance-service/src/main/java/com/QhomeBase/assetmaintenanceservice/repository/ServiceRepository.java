package com.QhomeBase.assetmaintenanceservice.repository;

import com.QhomeBase.assetmaintenanceservice.model.service.Service;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ServiceRepository extends JpaRepository<Service, UUID> {

    boolean existsByCodeIgnoreCase(String code);

    List<Service> findByCategoryIdAndIsActiveTrueOrderByNameAsc(UUID categoryId);

    List<Service> findByCategory_CodeIgnoreCaseAndIsActiveTrueOrderByNameAsc(String categoryCode);
    List<Service> findAllByIsActive(Boolean isActive);
}
