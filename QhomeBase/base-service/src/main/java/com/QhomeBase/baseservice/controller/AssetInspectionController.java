package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.*;
import com.QhomeBase.baseservice.model.InspectionStatus;
import com.QhomeBase.baseservice.security.UserPrincipal;
import com.QhomeBase.baseservice.service.AssetInspectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/asset-inspections")
@RequiredArgsConstructor
public class AssetInspectionController {

    private final AssetInspectionService inspectionService;

    @PostMapping
    public ResponseEntity<AssetInspectionDto> createInspection(
            @RequestBody CreateAssetInspectionRequest request) {
        AssetInspectionDto result = inspectionService.createInspection(request, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/contract/{contractId}")
    public ResponseEntity<AssetInspectionDto> getInspectionByContractId(@PathVariable UUID contractId) {
        try {
            AssetInspectionDto result = inspectionService.getInspectionByContractId(contractId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException ex) {

            if (ex.getMessage() != null && 
                (ex.getMessage().toLowerCase().contains("not found") || 
                 ex.getMessage().toLowerCase().contains("không tìm thấy"))) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            throw ex;
        }
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<AssetInspectionItemDto> updateInspectionItem(
            @PathVariable UUID itemId,
            @RequestBody UpdateAssetInspectionItemRequest request) {
        AssetInspectionItemDto result = inspectionService.updateInspectionItem(itemId, request, null);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{inspectionId}/start")
    public ResponseEntity<AssetInspectionDto> startInspection(@PathVariable UUID inspectionId) {
        AssetInspectionDto result = inspectionService.startInspection(inspectionId, null);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{inspectionId}/complete")
    public ResponseEntity<AssetInspectionDto> completeInspection(
            @PathVariable UUID inspectionId,
            @RequestBody(required = false) String inspectorNotes) {
        AssetInspectionDto result = inspectionService.completeInspection(inspectionId, inspectorNotes, null);
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<List<AssetInspectionDto>> getAllInspections(
            @RequestParam(required = false) UUID inspectorId,
            @RequestParam(required = false) InspectionStatus status) {
        List<AssetInspectionDto> result = inspectionService.getAllInspections(inspectorId, status);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/technician/{technicianId}")
    public ResponseEntity<List<AssetInspectionDto>> getInspectionsByTechnicianId(
            @PathVariable UUID technicianId) {
        List<AssetInspectionDto> result = inspectionService.getInspectionsByTechnicianId(technicianId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/my-assignments")
    public ResponseEntity<?> getMyAssignments(
            @AuthenticationPrincipal UserPrincipal principal) {
        try {
            if (principal == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            List<AssetInspectionDto> result = inspectionService.getMyAssignments(principal.uid());
            return ResponseEntity.ok(result);
        } catch (Exception ex) {
            System.err.println("Error in getMyAssignments: " + ex.getMessage());
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of(
                            "error", "Internal server error",
                            "message", ex.getMessage() != null ? ex.getMessage() : "Unknown error"
                    ));
        }
    }

    @PostMapping("/{inspectionId}/recalculate-damage")
    public ResponseEntity<AssetInspectionDto> recalculateDamageCost(@PathVariable UUID inspectionId) {
        AssetInspectionDto result = inspectionService.recalculateDamageCost(inspectionId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{inspectionId}/generate-invoice")
    public ResponseEntity<AssetInspectionDto> generateInvoice(
            @PathVariable UUID inspectionId,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID userId = principal != null ? principal.uid() : null;
        AssetInspectionDto result = inspectionService.generateInvoice(inspectionId, userId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/pending-approval")
    public ResponseEntity<List<AssetInspectionDto>> getInspectionsPendingApproval() {
        List<AssetInspectionDto> result = inspectionService.getInspectionsPendingApproval();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{inspectionId}/approve")
    public ResponseEntity<AssetInspectionDto> approveInspection(
            @PathVariable UUID inspectionId,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        AssetInspectionDto result = inspectionService.approveInspection(inspectionId, principal.uid());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{inspectionId}/reject")
    public ResponseEntity<AssetInspectionDto> rejectInspection(
            @PathVariable UUID inspectionId,
            @RequestBody(required = false) String rejectionNotes,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        AssetInspectionDto result = inspectionService.rejectInspection(inspectionId, rejectionNotes, principal.uid());
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{inspectionId}/scheduled-date")
    public ResponseEntity<AssetInspectionDto> updateScheduledDate(
            @PathVariable UUID inspectionId,
            @RequestBody UpdateAssetInspectionRequest request) {
        AssetInspectionDto result = inspectionService.updateScheduledDate(inspectionId, request.scheduledDate());
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{inspectionId}/assign-inspector")
    public ResponseEntity<AssetInspectionDto> assignInspector(
            @PathVariable UUID inspectionId,
            @RequestBody AssignInspectorRequest request) {
        AssetInspectionDto result = inspectionService.assignInspector(
                inspectionId, 
                request.inspectorId(), 
                request.inspectorName());
        return ResponseEntity.ok(result);
    }

}

