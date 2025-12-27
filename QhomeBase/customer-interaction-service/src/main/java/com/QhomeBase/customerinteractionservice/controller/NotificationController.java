package com.QhomeBase.customerinteractionservice.controller;

import com.QhomeBase.customerinteractionservice.dto.notification.*;
import com.QhomeBase.customerinteractionservice.service.NotificationDeviceTokenService;
import com.QhomeBase.customerinteractionservice.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationDeviceTokenService notificationDeviceTokenService;

    @PostMapping
    @PreAuthorize("@authz.canManageNotifications()")
    public ResponseEntity<NotificationResponse> createNotification(
            @Valid @RequestBody CreateNotificationRequest request) {
        
        NotificationResponse response = notificationService.createNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.canManageNotifications()")
    public ResponseEntity<NotificationResponse> updateNotification(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateNotificationRequest request) {
        
        NotificationResponse response = notificationService.updateNotification(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.canManageNotifications()")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable("id") UUID id,
            org.springframework.security.core.Authentication authentication) {
        
        com.QhomeBase.customerinteractionservice.security.UserPrincipal principal = 
            (com.QhomeBase.customerinteractionservice.security.UserPrincipal) authentication.getPrincipal();
        
        notificationService.deleteNotification(id, principal.uid());
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    // @PreAuthorize("@authz.canViewNotifications()")
    // @PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN') or hasRole('RESIDENT') or há")
    public ResponseEntity<List<NotificationResponse>> getAllNotifications() {
        List<NotificationResponse> notifications = notificationService.getAllNotifications();
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationDetailResponse> getNotificationById(@PathVariable("id") UUID id) {
        NotificationDetailResponse response = notificationService.getNotificationDetailById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/resident")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<?> getNotificationsForResident(
            @RequestParam(required = false) UUID buildingId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "7") int size,
            org.springframework.security.core.Authentication authentication) {
        
        // Get userId from authentication
        com.QhomeBase.customerinteractionservice.security.UserPrincipal principal = 
            (com.QhomeBase.customerinteractionservice.security.UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        // Get residentId from userId via service
        UUID residentId = notificationService.getResidentIdFromUserId(userId);
        if (residentId == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Resident not found for current user"));
        }
        
        // Ensure size is 7 as per requirement
        size = 7;
        
        com.QhomeBase.customerinteractionservice.dto.notification.NotificationPagedResponse pagedResponse = 
                notificationService.getNotificationsForResidentPaged(residentId, buildingId, page, size);
        return ResponseEntity.ok(pagedResponse);
    }

    @GetMapping("/resident/count")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<Map<String, Long>> getNotificationsCountForResident(
            @RequestParam(required = false) UUID buildingId,
            org.springframework.security.core.Authentication authentication) {
        
        // Get userId from authentication
        com.QhomeBase.customerinteractionservice.security.UserPrincipal principal = 
            (com.QhomeBase.customerinteractionservice.security.UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        // Get residentId from userId via service
        UUID residentId = notificationService.getResidentIdFromUserId(userId);
        if (residentId == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("totalCount", 0L));
        }
        
        long totalCount = notificationService.getNotificationsCountForResident(residentId, buildingId);
        return ResponseEntity.ok(java.util.Map.of("totalCount", totalCount));
    }

    @GetMapping("/role")
    @PreAuthorize("@authz.canViewNotifications()")
    public ResponseEntity<List<NotificationResponse>> getNotificationsForRole(
            @RequestParam String role,
            @RequestParam UUID userId) {
        
        List<NotificationResponse> notifications = notificationService.getNotificationsForRole(role, userId);
        return ResponseEntity.ok(notifications);
    }

    @PostMapping("/device-tokens")
    // Allow both authenticated and unauthenticated requests (for push notifications before login)
    public ResponseEntity<DeviceTokenResponse> registerDeviceToken(
            @Valid @RequestBody RegisterDeviceTokenRequest request,
            org.springframework.security.core.Authentication authentication) {

        UUID userId = null;
        if (authentication != null && authentication.getPrincipal() instanceof com.QhomeBase.customerinteractionservice.security.UserPrincipal) {
            var principal = (com.QhomeBase.customerinteractionservice.security.UserPrincipal) authentication.getPrincipal();
            userId = principal.uid();
        }

        RegisterDeviceTokenRequest effectiveRequest = RegisterDeviceTokenRequest.builder()
                .token(request.getToken())
                .platform(request.getPlatform())
                .appVersion(request.getAppVersion())
                .residentId(request.getResidentId())
                .buildingId(request.getBuildingId())
                .role(request.getRole())
                .userId(Optional.ofNullable(request.getUserId()).orElse(userId))
                .build();

        DeviceTokenResponse response = notificationDeviceTokenService.registerToken(effectiveRequest);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/device-tokens/{token}")
    // Allow both authenticated and unauthenticated requests (for logout/uninstall scenarios)
    public ResponseEntity<Void> deleteDeviceToken(@PathVariable String token) {
        notificationDeviceTokenService.removeToken(token);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/internal")
    public ResponseEntity<Void> createInternalNotification(
            @Valid @RequestBody com.QhomeBase.customerinteractionservice.dto.notification.InternalNotificationRequest request) {
        
        notificationService.createInternalNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/push-only")
    public ResponseEntity<Void> sendPushOnly(
            @RequestBody Map<String, Object> request) {
        
        UUID residentId = request.get("residentId") != null 
            ? UUID.fromString(request.get("residentId").toString()) 
            : null;
        String title = (String) request.get("title");
        String body = (String) request.get("message");
        
        @SuppressWarnings("unchecked")
        Map<String, String> data = request.get("data") != null 
            ? (Map<String, String>) request.get("data") 
            : new HashMap<>();
        
        if (residentId == null || title == null || body == null) {
            return ResponseEntity.badRequest().build();
        }
        
        notificationService.sendPushOnly(residentId, title, body, data);
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(org.springframework.security.access.AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(HttpStatus.FORBIDDEN.value(), "Access denied"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal server error: " + ex.getMessage()));
    }

    record ErrorResponse(int status, String message) {}
}















