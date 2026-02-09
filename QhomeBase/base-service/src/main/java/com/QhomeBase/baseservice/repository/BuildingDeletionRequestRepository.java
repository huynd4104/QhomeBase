package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.BuildingDeletionRequest;
import com.QhomeBase.baseservice.model.BuildingDeletionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BuildingDeletionRequestRepository extends JpaRepository<BuildingDeletionRequest, UUID> {
    List<BuildingDeletionRequest> findByBuildingId(UUID buildingId);

    List<BuildingDeletionRequest> findByStatus(BuildingDeletionStatus status);
}
