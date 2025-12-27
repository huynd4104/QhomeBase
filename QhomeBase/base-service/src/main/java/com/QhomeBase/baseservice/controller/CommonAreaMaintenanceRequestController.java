package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.CommonAreaMaintenanceRequestDto;
import com.QhomeBase.baseservice.dto.CreateCommonAreaMaintenanceRequestDto;
import com.QhomeBase.baseservice.dto.AdminCommonAreaMaintenanceResponseDto;
import com.QhomeBase.baseservice.dto.AdminServiceRequestActionDto;
import com.QhomeBase.baseservice.security.UserPrincipal;
import com.QhomeBase.baseservice.service.CommonAreaMaintenanceRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/common-area-maintenance-requests")
@RequiredArgsConstructor
@Slf4j
public class CommonAreaMaintenanceRequestController {

    private final CommonAreaMaintenanceRequestService service;

    @PostMapping
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<?> createRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateCommonAreaMaintenanceRequestDto requestDto) {
        try {
            CommonAreaMaintenanceRequestDto created = service.create(principal.uid(), requestDto);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.warn("Failed to create common area maintenance request: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<List<CommonAreaMaintenanceRequestDto>> getMyRequests(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<CommonAreaMaintenanceRequestDto> requests = service.getMyRequests(principal.uid());
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/admin/pending")
    @PreAuthorize("@authz.canManageServiceRequests()")
    public ResponseEntity<List<CommonAreaMaintenanceRequestDto>> getPendingRequests() {
        return ResponseEntity.ok(service.getPendingRequests());
    }

    @GetMapping("/admin/in-progress")
    @PreAuthorize("@authz.canManageServiceRequests()")
    public ResponseEntity<List<CommonAreaMaintenanceRequestDto>> getInProgressRequests() {
        return ResponseEntity.ok(service.getInProgressRequests());
    }

    @GetMapping("/admin/status/{status}")
    @PreAuthorize("@authz.canManageServiceRequests()")
    public ResponseEntity<List<CommonAreaMaintenanceRequestDto>> getRequestsByStatus(
            @PathVariable String status) {
        return ResponseEntity.ok(service.getRequestsByStatus(status));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("@authz.canManageServiceRequests()")
    public ResponseEntity<List<CommonAreaMaintenanceRequestDto>> getAllRequests() {
        return ResponseEntity.ok(service.getAllRequests());
    }

    @GetMapping("/{requestId}")
    @PreAuthorize("@authz.canManageServiceRequests()")
    public ResponseEntity<CommonAreaMaintenanceRequestDto> getRequestById(@PathVariable UUID requestId) {
        CommonAreaMaintenanceRequestDto request = service.getRequestById(requestId);
        return ResponseEntity.ok(request);
    }

    @PostMapping("/admin/{requestId}/approve")
    @PreAuthorize("@authz.canManageServiceRequests()")
    public ResponseEntity<CommonAreaMaintenanceRequestDto> approveRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID requestId,
            @RequestBody(required = false) AdminCommonAreaMaintenanceResponseDto request) {
        try {
            // Đơn giản: chỉ đổi status sang IN_PROGRESS
            AdminCommonAreaMaintenanceResponseDto dto = request != null 
                    ? request 
                    : new AdminCommonAreaMaintenanceResponseDto(null);
            CommonAreaMaintenanceRequestDto result = service.respondToRequest(
                    principal.uid(),
                    requestId,
                    dto
            );
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.warn("Failed to approve common area maintenance request: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PatchMapping("/admin/{requestId}/deny")
    @PreAuthorize("@authz.canManageServiceRequests()")
    public ResponseEntity<CommonAreaMaintenanceRequestDto> denyRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID requestId,
            @RequestBody(required = false) AdminServiceRequestActionDto request) {
        try {
            CommonAreaMaintenanceRequestDto dto = service.denyRequest(
                    principal.uid(),
                    requestId,
                    request
            );
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.warn("Failed to deny common area maintenance request: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PatchMapping("/admin/{requestId}/complete")
    @PreAuthorize("@authz.canManageServiceRequests()")
    public ResponseEntity<CommonAreaMaintenanceRequestDto> completeRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID requestId,
            @RequestBody(required = false) AdminServiceRequestActionDto request) {
        // Đơn giản: chỉ đổi status sang COMPLETED
        CommonAreaMaintenanceRequestDto dto = service.completeRequest(
                principal.uid(),
                requestId,
                request
        );
        return ResponseEntity.ok(dto);
    }

    // Removed approve-response and reject-response endpoints - không cần resident approve/reject response nữa
    // Admin/Staff approve/deny trực tiếp qua /admin/{requestId}/respond và /admin/{requestId}/deny

    @PatchMapping("/{requestId}/cancel")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<CommonAreaMaintenanceRequestDto> cancelRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID requestId) {
        CommonAreaMaintenanceRequestDto dto = service.cancelRequest(principal.uid(), requestId);
        return ResponseEntity.ok(dto);
    }

    // Removed assign and add-progress-note endpoints - đơn giản hóa luồng
}
