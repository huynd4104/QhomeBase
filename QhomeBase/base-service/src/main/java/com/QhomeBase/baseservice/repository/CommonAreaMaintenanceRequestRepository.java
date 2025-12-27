package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.CommonAreaMaintenanceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommonAreaMaintenanceRequestRepository extends 
        JpaRepository<CommonAreaMaintenanceRequest, UUID>,
        JpaSpecificationExecutor<CommonAreaMaintenanceRequest> {
    
    List<CommonAreaMaintenanceRequest> findByResidentIdOrderByCreatedAtDesc(UUID residentId);
    
    List<CommonAreaMaintenanceRequest> findByStatusOrderByCreatedAtDesc(String status);
    
    List<CommonAreaMaintenanceRequest> findByBuildingIdOrderByCreatedAtDesc(UUID buildingId);
    
    List<CommonAreaMaintenanceRequest> findByResidentIdAndStatusNotIn(
            UUID residentId, 
            List<String> excludedStatuses
    );
}
