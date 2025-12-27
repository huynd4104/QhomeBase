package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.Service;
import com.QhomeBase.baseservice.model.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceRepository extends JpaRepository<Service, UUID> {
    
    Optional<Service> findByCode(String code);
    
    List<Service> findByActive(Boolean active);
    
    List<Service> findByRequiresMeter(Boolean requiresMeter);
    
    List<Service> findByType(ServiceType type);
    
    List<Service> findByActiveAndRequiresMeter(Boolean active, Boolean requiresMeter);
}

