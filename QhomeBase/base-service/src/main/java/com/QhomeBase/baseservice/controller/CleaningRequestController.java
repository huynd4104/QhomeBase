package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.AdminServiceRequestActionDto;
import com.QhomeBase.baseservice.dto.CleaningRequestConfigDto;
import com.QhomeBase.baseservice.dto.CleaningRequestDto;
import com.QhomeBase.baseservice.dto.CreateCleaningRequestDto;
import com.QhomeBase.baseservice.security.UserPrincipal;
import com.QhomeBase.baseservice.service.CleaningRequestService;
import com.QhomeBase.baseservice.service.CleaningRequestMonitor;
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
@RequestMapping("/api/cleaning-requests")
@RequiredArgsConstructor
@Slf4j
public class CleaningRequestController {

    private final CleaningRequestService cleaningRequestService;
    private final CleaningRequestMonitor cleaningRequestMonitor;

    @PostMapping
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<?> createCleaningRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateCleaningRequestDto requestDto) {
        try {
            CleaningRequestDto created = cleaningRequestService.create(principal.uid(), requestDto);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.warn("Failed to create cleaning request: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(java.util.Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/config")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<CleaningRequestConfigDto> getCleaningRequestConfig() {
        CleaningRequestConfigDto config = cleaningRequestMonitor.getConfig();
        return ResponseEntity.ok(config);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<?> getMyCleaningRequests(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset) {
        // If pagination parameters are provided, return paginated response
        if (limit != null && offset != null) {
            Map<String, Object> pagedResponse = cleaningRequestService.getMyRequestsPaged(
                    principal.uid(), limit, offset);
            return ResponseEntity.ok(pagedResponse);
        }
        // Otherwise, return all requests (backward compatibility)
        List<CleaningRequestDto> requests = cleaningRequestService.getMyRequests(principal.uid());
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/my/paid")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<List<CleaningRequestDto>> getPaidCleaningRequests(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<CleaningRequestDto> requests = cleaningRequestService.getPaidRequests(principal.uid());
        return ResponseEntity.ok(requests);
    }

    @PostMapping("/{requestId}/resend")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<CleaningRequestDto> resendCleaningRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID requestId) {
        CleaningRequestDto dto = cleaningRequestService.resendRequest(principal.uid(), requestId);
        return ResponseEntity.ok(dto);
    }

    @PatchMapping("/{requestId}/cancel")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<CleaningRequestDto> cancelCleaningRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID requestId) {
        CleaningRequestDto dto = cleaningRequestService.cancelRequest(principal.uid(), requestId);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/admin/pending")
    @PreAuthorize("@authz.canManageServiceRequests()")
    public ResponseEntity<List<CleaningRequestDto>> getPendingCleaningRequests() {
        return ResponseEntity.ok(cleaningRequestService.getPendingRequests());
    }

    @PatchMapping("/admin/{requestId}/approve")
    @PreAuthorize("@authz.canManageServiceRequests()")
    public ResponseEntity<CleaningRequestDto> approveCleaningRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID requestId,
            @RequestBody(required = false) AdminServiceRequestActionDto request) {
        CleaningRequestDto dto = cleaningRequestService.approveRequest(
                principal.uid(),
                requestId,
                request
        );
        return ResponseEntity.ok(dto);
    }

    @PatchMapping("/admin/{requestId}/complete")
    @PreAuthorize("@authz.canManageServiceRequests()")
    public ResponseEntity<CleaningRequestDto> completeCleaningRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID requestId,
            @RequestBody(required = false) AdminServiceRequestActionDto request) {
        CleaningRequestDto dto = cleaningRequestService.completeRequest(
                principal.uid(),
                requestId,
                request
        );
        return ResponseEntity.ok(dto);
    }
}

