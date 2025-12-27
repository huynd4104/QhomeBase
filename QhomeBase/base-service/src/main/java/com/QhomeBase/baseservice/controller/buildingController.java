package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.BuildingCreateReq;
import com.QhomeBase.baseservice.dto.BuildingDto;
import com.QhomeBase.baseservice.dto.BuildingUpdateReq;
import com.QhomeBase.baseservice.dto.BuildingDeletionRequestDto;
import com.QhomeBase.baseservice.dto.BuildingDeletionCreateReq;
import com.QhomeBase.baseservice.dto.BuildingDeletionApproveReq;
import com.QhomeBase.baseservice.dto.BuildingDeletionRejectReq;
import com.QhomeBase.baseservice.model.Building;
import com.QhomeBase.baseservice.model.BuildingStatus;
import com.QhomeBase.baseservice.security.UserPrincipal;
import com.QhomeBase.baseservice.security.AuthzService;
import com.QhomeBase.baseservice.service.BuildingService;
import com.QhomeBase.baseservice.service.BuildingDeletionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/buildings")
@RequiredArgsConstructor
public class buildingController {

    private final BuildingService buildingService;
    private final BuildingDeletionService buildingDeletionService;
    private final AuthzService authzService;


    @GetMapping
    public ResponseEntity<List<Building>> findAll() {
        List<Building> buildings = buildingService.findAllOrderByCodeAsc();
        return ResponseEntity.ok(buildings);
    }

    @GetMapping("/{buildingId}")
    public ResponseEntity<BuildingDto> getBuildingById(@PathVariable UUID buildingId) {
        try {
            BuildingDto building = buildingService.getBuildingById(buildingId);
            return ResponseEntity.ok(building);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @PreAuthorize("@authz.canCreateBuilding()")
    public ResponseEntity<BuildingDto> createBuilding(
            @Valid @RequestBody BuildingCreateReq req, 
            Authentication auth) {
        var user = (UserPrincipal) auth.getPrincipal();
        BuildingDto createdBuilding = buildingService.createBuilding(req, user.username());
        return ResponseEntity.ok(createdBuilding);
    }


    @PutMapping("/{buildingId}")
    @PreAuthorize("@authz.canUpdateBuilding()")
    public ResponseEntity<BuildingDto> updateBuilding(
            @PathVariable("buildingId") UUID buildingId,
            @Valid @RequestBody BuildingUpdateReq req,
            Authentication auth) {
        try {
            BuildingDto updatedBuilding = buildingService.updateBuilding(buildingId, req, auth);
            return ResponseEntity.ok(updatedBuilding);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{buildingId}/status")
    @PreAuthorize("@authz.canUpdateBuilding()")
    public ResponseEntity<Void> changeBuildingStatus(
            @PathVariable("buildingId") UUID buildingId,
            @RequestParam BuildingStatus status,
            Authentication auth) {
        try {
            buildingService.changeBuildingStatus(buildingId, status, auth);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }


    
    @PostMapping("/{buildingId}/deletion-request")
    @PreAuthorize("@authz.canRequestDeleteBuilding(#buildingId)")
    public ResponseEntity<BuildingDeletionRequestDto> createBuildingDeletionRequest(
            @PathVariable UUID buildingId,
            @Valid @RequestBody BuildingDeletionCreateReq req,
            Authentication auth) {
        try {
            BuildingDeletionRequestDto request = buildingDeletionService.createBuildingDeletionRequest(
                    buildingId, req.reason(), auth);
            return ResponseEntity.ok(request);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/deletion-requests/{requestId}/approve")
    @PreAuthorize("@authz.canApproveBuildingDeletion()")
    public ResponseEntity<BuildingDeletionRequestDto> approveBuildingDeletionRequest(
            @PathVariable UUID requestId,
            @Valid @RequestBody BuildingDeletionApproveReq req,
            Authentication auth) {
        try {
            BuildingDeletionRequestDto request = buildingDeletionService.approveBuildingDeletionRequest(
                    requestId, req.note(), auth);
            return ResponseEntity.ok(request);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/deletion-requests/{requestId}/reject")
    @PreAuthorize("@authz.canApproveBuildingDeletion()")
    public ResponseEntity<BuildingDeletionRequestDto> rejectBuildingDeletionRequest(
            @PathVariable UUID requestId,
            @Valid @RequestBody BuildingDeletionRejectReq req,
            Authentication auth) {
        try {
            BuildingDeletionRequestDto request = buildingDeletionService.rejectBuildingDeletionRequest(
                    requestId, req.note(), auth);
            return ResponseEntity.ok(request);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/deletion-requests/pending")
    @PreAuthorize("@authz.canViewAllDeleteBuildings()")
    public ResponseEntity<List<BuildingDeletionRequestDto>> getPendingBuildingDeletionRequests() {
        return ResponseEntity.ok(buildingDeletionService.getPendingRequests());
    }

    @GetMapping("/deletion-requests/{requestId}")
    public ResponseEntity<BuildingDeletionRequestDto> getBuildingDeletionRequest(
            @PathVariable UUID requestId,
            Authentication auth) {
        try {
            BuildingDeletionRequestDto request = buildingDeletionService.getById(requestId);
            return ResponseEntity.ok(request);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{buildingId}/do")
    @PreAuthorize("@authz.canRequestDeleteBuilding(#buildingId)")
    public ResponseEntity<String> doBuildingDeletion(@PathVariable UUID buildingId, Authentication auth) {
        buildingDeletionService.doBuildingDeletion(buildingId, auth);
        return ResponseEntity.ok("Building deletion completed successfully");
    }

    @GetMapping("/deleting")
    public List<BuildingDeletionRequestDto> getDeletingBuildings() {
        if (!authzService.canViewAllDeleteBuildings()) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied");
        }
        return buildingDeletionService.getDeletingBuildings();
    }

    @GetMapping("/all")
    public List<BuildingDeletionRequestDto> getAllBuildingDeletionRequests() {
        if (!authzService.canViewAllDeleteBuildings()) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied");
        }
        return buildingDeletionService.getAllBuildingDeletionRequests();
    }

    @GetMapping("/my-deleting-buildings")
    public List<BuildingDeletionRequestDto> getMyDeletingBuildings(Authentication auth) {
        return buildingDeletionService.getDeletingBuildings();
    }

    @GetMapping("/my-all")
    public List<BuildingDeletionRequestDto> getMyAllBuildingDeletionRequests(Authentication auth) {
        return buildingDeletionService.getAllBuildingDeletionRequests();
    }

    @GetMapping("/my-deleting-buildings-raw")
    public ResponseEntity<List<Building>> getMyDeletingBuildingsRaw(Authentication auth) {
        var deletingBuildings = buildingDeletionService.getDeletingBuildingsRaw();
        return ResponseEntity.ok(deletingBuildings);
    }
    
    @GetMapping("/{buildingId}/targets-status")
    public ResponseEntity<Map<String, Object>> getBuildingDeletionTargetsStatus(@PathVariable UUID buildingId, Authentication auth) {
        return ResponseEntity.ok(buildingDeletionService.getBuildingDeletionTargetsStatus(buildingId));
    }

    @PostMapping("/{requestId}/complete")
    @PreAuthorize("@authz.canCompleteBuildingDeletion()")
    public ResponseEntity<BuildingDeletionRequestDto> completeBuildingDeletion(@PathVariable UUID requestId, Authentication auth) {
        return ResponseEntity.ok(buildingDeletionService.completeBuildingDeletion(requestId, auth));
    }

}
