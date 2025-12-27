package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.BuildingDeletionRequestDto;
import com.QhomeBase.baseservice.model.BuildingDeletionRequest;
import com.QhomeBase.baseservice.model.BuildingDeletionStatus;
import com.QhomeBase.baseservice.model.BuildingStatus;
import com.QhomeBase.baseservice.model.Unit;
import com.QhomeBase.baseservice.model.UnitStatus;
import com.QhomeBase.baseservice.repository.BuildingDeletionRequestRepository;
import com.QhomeBase.baseservice.repository.UnitRepository;
import com.QhomeBase.baseservice.repository.BuildingRepository;
import com.QhomeBase.baseservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BuildingDeletionService {
    private final BuildingDeletionRequestRepository repo;
    private final BuildingRepository buildingRepository;
    private final UnitRepository unitRepository;

    private static BuildingDeletionRequestDto toDto(BuildingDeletionRequest t) {
        return new BuildingDeletionRequestDto(
                t.getId(),
                t.getBuildingId(),
                t.getRequestedBy(),
                t.getReason(),
                t.getApprovedBy(),
                t.getNote(),
                t.getStatus(),
                t.getCreatedAt(),
                t.getApprovedAt()
        );
    }

    public void doBuildingDeletion(UUID buildingId, Authentication auth) {
        var building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new IllegalArgumentException("Building not found"));
        
        if (building.getStatus() != BuildingStatus.INACTIVE) {
            throw new IllegalStateException("Building must be INACTIVE before performing deletion tasks");
        }

        inactivateActiveUnitsOfBuilding(buildingId);
    }

    public BuildingDeletionRequestDto getById(UUID requestId) {
        var e = repo.findById(requestId).orElseThrow(() -> new IllegalArgumentException("Request not found"));
        return toDto(e);
    }

    public List<BuildingDeletionRequestDto> getByBuildingId(UUID buildingId) {
        return repo.findByBuildingId(buildingId)
                .stream()
                .map(BuildingDeletionService::toDto)
                .toList();
    }

    public List<BuildingDeletionRequestDto> getPendingRequests() {
        return repo.findByStatus(BuildingDeletionStatus.PENDING)
                .stream()
                .map(BuildingDeletionService::toDto)
                .toList();
    }

    public List<BuildingDeletionRequestDto> getDeletingBuildings() {
        var inactiveBuildings = buildingRepository.findAll()
                .stream()
                .filter(building -> building.getStatus() == BuildingStatus.INACTIVE)
                .toList();
        
        return repo.findAll()
                .stream()
                .filter(req -> inactiveBuildings.stream()
                        .anyMatch(building -> building.getId().equals(req.getBuildingId())))
                .map(BuildingDeletionService::toDto)
                .toList();
    }

    public List<BuildingDeletionRequestDto> getAllBuildingDeletionRequests() {
        return repo.findAll()
                .stream()
                .map(BuildingDeletionService::toDto)
                .toList();
    }

    public List<com.QhomeBase.baseservice.model.Building> getDeletingBuildingsRaw() {
        return buildingRepository.findAll()
                .stream()
                .filter(building -> building.getStatus() == BuildingStatus.INACTIVE)
                .toList();
    }

    public void changeStatusOfUnitsBuilding(UUID buildingId) {
        var b = unitRepository.findAllByBuildingId(buildingId);
        for (Unit u : b) {
            if (u.getStatus().equals(UnitStatus.ACTIVE)) {
                u.setStatus(UnitStatus.INACTIVE);
            }
        }
        unitRepository.saveAll(b);
    }
    
    public void inactivateActiveUnitsOfBuilding(UUID buildingId) {
        var units = unitRepository.findAllByBuildingId(buildingId);
        boolean changed = false;
        for (Unit u : units) {
            if (u.getStatus() == UnitStatus.ACTIVE) {
                u.setStatus(UnitStatus.INACTIVE);
                changed = true;
            }
        }
        if (changed) unitRepository.saveAll(units);
    }

    public Map<String, Object> getBuildingDeletionTargetsStatus(UUID buildingId) {
        List<Unit> units = unitRepository.findAllByBuildingId(buildingId);
        
        Map<String, Object> status = new HashMap<>();
        
        Map<String, Long> unitStatusCount = units.stream()
                .collect(Collectors.groupingBy(
                    u -> u.getStatus().toString(),
                    Collectors.counting()
                ));
        
        long unitsInactive = unitStatusCount.getOrDefault(UnitStatus.INACTIVE.name(), 0L);
        
        boolean unitsReady = units.isEmpty() || unitsInactive == units.size();
        boolean allTargetsReady = unitsReady;
        
        status.put("units", unitStatusCount);
        status.put("totalUnits", units.size());
        status.put("unitsInactive", unitsInactive);
        status.put("unitsReady", unitsReady);
        status.put("allTargetsReady", allTargetsReady);
        status.put("requirements", Map.of(
            "units", "All units must be INACTIVE"
        ));
        
        return status;
    }

    public BuildingDeletionRequestDto createBuildingDeletionRequest(UUID buildingId, String reason, Authentication auth) {
        var user = (UserPrincipal) auth.getPrincipal();
        
        var building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new IllegalArgumentException("Building not found with ID: " + buildingId));
        
        if (building.getStatus() == BuildingStatus.INACTIVE || building.getStatus() == BuildingStatus.ARCHIVED) {
            throw new IllegalStateException("Building is already in deletion process or archived");
        }
        
        var existingRequests = repo.findByBuildingId(buildingId);
        boolean hasActiveRequest = existingRequests.stream()
                .anyMatch(req -> req.getStatus() == BuildingDeletionStatus.PENDING 
                        || req.getStatus() == BuildingDeletionStatus.APPROVED);
        
        if (hasActiveRequest) {
            throw new IllegalStateException("There is already an active deletion request for this building");
        }
        
        building.setStatus(BuildingStatus.PENDING_DELETION);
        buildingRepository.save(building);
        
        var request = BuildingDeletionRequest.builder()
                .buildingId(buildingId)
                .requestedBy(user.uid())
                .reason(reason != null ? reason : "Building deletion request")
                .status(BuildingDeletionStatus.PENDING)
                .createdAt(OffsetDateTime.now())
                .build();
        
        repo.save(request);
        return toDto(request);
    }

    public BuildingDeletionRequestDto approveBuildingDeletionRequest(UUID requestId, String note, Authentication auth) {
        var user = (UserPrincipal) auth.getPrincipal();
        
        var request = repo.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Building deletion request not found with ID: " + requestId));
        
        if (request.getStatus() != BuildingDeletionStatus.PENDING) {
            throw new IllegalStateException("Only PENDING requests can be approved");
        }
        
        var building = buildingRepository.findById(request.getBuildingId())
                .orElseThrow(() -> new IllegalArgumentException("Building not found"));
        
        building.setStatus(BuildingStatus.INACTIVE);
        inactivateActiveUnitsOfBuilding(request.getBuildingId());
        buildingRepository.save(building);
        
        request.setStatus(BuildingDeletionStatus.APPROVED);
        request.setApprovedBy(user.uid());
        request.setNote(note);
        request.setApprovedAt(OffsetDateTime.now());
        repo.save(request);
        
        return toDto(request);
    }

    public BuildingDeletionRequestDto rejectBuildingDeletionRequest(UUID requestId, String note, Authentication auth) {
        var user = (UserPrincipal) auth.getPrincipal();
        
        var request = repo.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Building deletion request not found with ID: " + requestId));
        
        if (request.getStatus() != BuildingDeletionStatus.PENDING) {
            throw new IllegalStateException("Only PENDING requests can be rejected");
        }
        
        var building = buildingRepository.findById(request.getBuildingId())
                .orElseThrow(() -> new IllegalArgumentException("Building not found"));
        
        building.setStatus(BuildingStatus.ACTIVE);
        buildingRepository.save(building);
        
        request.setStatus(BuildingDeletionStatus.REJECTED);
        request.setApprovedBy(user.uid());
        request.setNote(note);
        request.setApprovedAt(OffsetDateTime.now());
        repo.save(request);
        
        return toDto(request);
    }

    public BuildingDeletionRequestDto completeBuildingDeletion(UUID requestId, Authentication auth) {
        var request = repo.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Building deletion request not found with ID: " + requestId));
        
        if (request.getStatus() != BuildingDeletionStatus.APPROVED) {
            throw new IllegalStateException("Request must be APPROVED before completion");
        }
        
        Map<String, Object> targetsStatus = getBuildingDeletionTargetsStatus(request.getBuildingId());
        boolean allTargetsReady = (Boolean) targetsStatus.get("allTargetsReady");
        
        if (!allTargetsReady) {
            throw new IllegalStateException("All targets must be INACTIVE before completion. Current status: " + targetsStatus);
        }
        
        var building = buildingRepository.findById(request.getBuildingId())
                .orElseThrow(() -> new IllegalArgumentException("Building not found with ID: " + request.getBuildingId()));
        building.setStatus(BuildingStatus.ARCHIVED);
        buildingRepository.save(building);
        
        request.setStatus(BuildingDeletionStatus.COMPLETED);
        repo.save(request);
        
        return toDto(request);
    }

}
