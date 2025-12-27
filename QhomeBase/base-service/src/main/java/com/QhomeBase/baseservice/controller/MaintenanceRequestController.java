package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.AdminMaintenanceResponseDto;
import com.QhomeBase.baseservice.dto.AdminServiceRequestActionDto;
import com.QhomeBase.baseservice.dto.AddProgressNoteDto;
import com.QhomeBase.baseservice.dto.CreateMaintenanceRequestDto;
import com.QhomeBase.baseservice.dto.MaintenanceRequestConfigDto;
import com.QhomeBase.baseservice.dto.MaintenanceRequestDto;
import com.QhomeBase.baseservice.dto.VnpayUrlResponseDto;
import com.QhomeBase.baseservice.service.vnpay.VnpayPaymentResult;
import com.QhomeBase.baseservice.service.vnpay.VnpayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import com.QhomeBase.baseservice.security.UserPrincipal;
import com.QhomeBase.baseservice.service.MaintenanceRequestMonitor;
import com.QhomeBase.baseservice.service.MaintenanceRequestService;
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
@RequestMapping("/api/maintenance-requests")
@RequiredArgsConstructor
@Slf4j
public class MaintenanceRequestController {

    private final MaintenanceRequestService maintenanceRequestService;
    private final VnpayService vnpayService;
    private final MaintenanceRequestMonitor maintenanceRequestMonitor;

    @PostMapping
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<?> createMaintenanceRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateMaintenanceRequestDto requestDto) {
        try {
            MaintenanceRequestDto created = maintenanceRequestService.create(principal.uid(), requestDto);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.warn("Failed to create maintenance request: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(java.util.Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<?> getMyMaintenanceRequests(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset) {
        // If pagination parameters are provided, return paginated response
        if (limit != null && offset != null) {
            Map<String, Object> pagedResponse = maintenanceRequestService.getMyRequestsPaged(
                    principal.uid(), limit, offset);
            return ResponseEntity.ok(pagedResponse);
        }
        // Otherwise, return all requests (backward compatibility)
        List<MaintenanceRequestDto> requests = maintenanceRequestService.getMyRequests(principal.uid());
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/my/paid")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<List<MaintenanceRequestDto>> getPaidMaintenanceRequests(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<MaintenanceRequestDto> requests = maintenanceRequestService.getPaidRequests(principal.uid());
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/admin/new")
    @PreAuthorize("@authz.canManageServiceRequests()")
    public ResponseEntity<List<MaintenanceRequestDto>> getPendingMaintenanceRequests() {
        return ResponseEntity.ok(maintenanceRequestService.getPendingRequests());
    }

    @GetMapping("/admin/in-progress")
    @PreAuthorize("@authz.canManageServiceRequests()")
    public ResponseEntity<List<MaintenanceRequestDto>> getInProgressMaintenanceRequests() {
        return ResponseEntity.ok(maintenanceRequestService.getInProgressRequests());
    }

    @GetMapping("/admin/status/{status}")
    @PreAuthorize("@authz.canManageServiceRequests()")
    public ResponseEntity<List<MaintenanceRequestDto>> getMaintenanceRequestsByStatus(
            @PathVariable String status) {
        return ResponseEntity.ok(maintenanceRequestService.getRequestsByStatus(status));
    }

    @PostMapping("/admin/{requestId}/respond")
    @PreAuthorize("@authz.canManageServiceRequests()")
    public ResponseEntity<MaintenanceRequestDto> respondToMaintenanceRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID requestId,
            @Valid @RequestBody AdminMaintenanceResponseDto request) {
        try {
            MaintenanceRequestDto dto = maintenanceRequestService.respondToRequest(
                    principal.uid(),
                    requestId,
                    request
            );
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.warn("Failed to respond to maintenance request: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PatchMapping("/admin/{requestId}/deny")
    @PreAuthorize("@authz.canManageServiceRequests()")
    public ResponseEntity<MaintenanceRequestDto> denyMaintenanceRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID requestId,
            @Valid @RequestBody AdminServiceRequestActionDto request) {
        try {
            MaintenanceRequestDto dto = maintenanceRequestService.denyRequest(
                    principal.uid(),
                    requestId,
                    request
            );
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException ex) {
            log.warn("Failed to deny maintenance request: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PatchMapping("/admin/{requestId}/complete")
    @PreAuthorize("@authz.canManageServiceRequests()")
    public ResponseEntity<MaintenanceRequestDto> completeMaintenanceRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID requestId,
            @RequestBody(required = false) AdminServiceRequestActionDto request) {
        MaintenanceRequestDto dto = maintenanceRequestService.completeRequest(
                principal.uid(),
                requestId,
                request
        );
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{requestId}/approve-response")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<MaintenanceRequestDto> approveResponse(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID requestId) {
        try {
            MaintenanceRequestDto dto = maintenanceRequestService.approveResponse(principal.uid(), requestId);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.warn("Failed to approve response: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/{requestId}/reject-response")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<MaintenanceRequestDto> rejectResponse(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID requestId) {
        try {
            MaintenanceRequestDto dto = maintenanceRequestService.rejectResponse(principal.uid(), requestId);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.warn("Failed to reject response: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PatchMapping("/{requestId}/cancel")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<MaintenanceRequestDto> cancelMaintenanceRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID requestId) {
        MaintenanceRequestDto dto = maintenanceRequestService.cancelRequest(principal.uid(), requestId);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{requestId}/resend")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<?> resendMaintenanceRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID requestId) {
        try {
            MaintenanceRequestDto dto = maintenanceRequestService.resendRequest(principal.uid(), requestId);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.warn("Failed to resend maintenance request: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(java.util.Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/config")
    public ResponseEntity<MaintenanceRequestConfigDto> getConfig() {
        MaintenanceRequestConfigDto config = maintenanceRequestMonitor.getConfig();
        return ResponseEntity.ok(config);
    }

    @GetMapping("/all")
    @PreAuthorize("@authz.canManageServiceRequests()")
    public ResponseEntity<List<MaintenanceRequestDto>> getAllRequests() {
        List<MaintenanceRequestDto> requests = maintenanceRequestService.getAllRequests();
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/{requestId}")
    @PreAuthorize("@authz.canManageServiceRequests()")
    public ResponseEntity<MaintenanceRequestDto> getRequestById(@PathVariable UUID requestId) {
        MaintenanceRequestDto request = maintenanceRequestService.getRequestById(requestId);
        return ResponseEntity.ok(request);
    }

    @PostMapping("/admin/{requestId}/add-progress-note")
    @PreAuthorize("@authz.canManageServiceRequests()")
    public ResponseEntity<MaintenanceRequestDto> addProgressNote(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID requestId,
            @Valid @RequestBody AddProgressNoteDto request) {
        try {
            MaintenanceRequestDto dto = maintenanceRequestService.addProgressNote(
                    principal.uid(),
                    requestId,
                    request.note(),
                    request.cost()
            );
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.warn("Failed to add progress note: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/{requestId}/vnpay-url")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<VnpayUrlResponseDto> createVnpayUrl(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID requestId,
            HttpServletRequest request) {
        try {
            String clientIp = getClientIp(request);
            VnpayPaymentResult result = maintenanceRequestService.createVnpayPaymentUrl(
                    principal.uid(),
                    requestId,
                    clientIp
            );
            return ResponseEntity.ok(new VnpayUrlResponseDto(result.paymentUrl(), result.transactionRef()));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.warn("Failed to create VNPay URL: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/vnpay/redirect")
    public ResponseEntity<Map<String, Object>> handleVnpayRedirect(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        try {
            Map<String, String> params = vnpayService.extractParams(request);
            MaintenanceRequestDto dto = maintenanceRequestService.handleVnpayCallback(params);
            
            // Redirect to Flutter app deep link
            String requestId = dto.id() != null ? dto.id().toString() : "";
            String redirectUrl = new StringBuilder("qhomeapp://vnpay-maintenance-result")
                    .append("?requestId=").append(requestId)
                    .append("&success=true")
                    .append("&message=").append(URLEncoder.encode("Thanh toán thành công", StandardCharsets.UTF_8))
                    .toString();
            
            response.sendRedirect(redirectUrl);
            
            return ResponseEntity.status(HttpStatus.OK).body(Map.of(
                    "success", true,
                    "message", "Payment successful",
                    "requestId", requestId
            ));
        } catch (Exception ex) {
            log.error("VNPay callback error: {}", ex.getMessage(), ex);
            
            // Redirect to Flutter app with error
            String errorMessage = ex.getMessage() != null ? ex.getMessage() : "Unknown error";
            String encodedMessage = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
            String fallback = "qhomeapp://vnpay-maintenance-result?success=false&message=" + encodedMessage;
            response.sendRedirect(fallback);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", errorMessage
            ));
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}

