package com.QhomeBase.customerinteractionservice.service;

import com.QhomeBase.customerinteractionservice.dto.notification.CreateNotificationRequest;
import com.QhomeBase.customerinteractionservice.dto.notification.NotificationDetailResponse;
import com.QhomeBase.customerinteractionservice.dto.notification.NotificationPagedResponse;
import com.QhomeBase.customerinteractionservice.dto.notification.NotificationResponse;
import com.QhomeBase.customerinteractionservice.dto.notification.NotificationWebSocketMessage;
import com.QhomeBase.customerinteractionservice.dto.notification.UpdateNotificationRequest;
import com.QhomeBase.customerinteractionservice.model.Notification;
import com.QhomeBase.customerinteractionservice.model.NotificationScope;
import com.QhomeBase.customerinteractionservice.model.NotificationType;
import com.QhomeBase.customerinteractionservice.repository.NotificationRepository;
import com.QhomeBase.customerinteractionservice.client.BaseServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationPushService notificationPushService;
    private final NotificationDeviceTokenService deviceTokenService;
    private final BaseServiceClient baseServiceClient;

    public NotificationResponse createNotification(CreateNotificationRequest request) {
        validateNotificationScope(request.getScope(), request.getTargetRole(), request.getTargetBuildingId());

        Notification notification = Notification.builder()
                .type(request.getType())
                .title(request.getTitle())
                .message(request.getMessage())
                .scope(request.getScope())
                .targetRole(request.getTargetRole())
                .targetBuildingId(request.getTargetBuildingId())
                .targetResidentId(request.getTargetResidentId())
                .referenceId(request.getReferenceId())
                .referenceType(request.getReferenceType())
                .actionUrl(request.getActionUrl())
                .iconUrl(request.getIconUrl())
                .build();


        Notification savedNotification = notificationRepository.save(notification);

        // Chỉ gửi realtime notification và FCM push khi:
        // 1. scope = EXTERNAL (riêng tư cho cư dân)
        // 2. createdAt <= now (realtime)
        if (shouldSendNotification(savedNotification)) {
            sendWebSocketNotification(savedNotification, "NOTIFICATION_CREATED");
            notificationPushService.sendPushNotification(savedNotification);
            log.info("✅ [NotificationService] Sent realtime and FCM push notification for notification {} (EXTERNAL, createdAt <= now)", savedNotification.getId());
        } else {
            log.info("⏭️ [NotificationService] Skipped sending notification for notification {} (scope={}, createdAt={})", 
                    savedNotification.getId(), savedNotification.getScope(), savedNotification.getCreatedAt());
        }

        return toResponse(savedNotification);
    }

    public NotificationResponse updateNotification(UUID id, UpdateNotificationRequest request) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found with ID: " + id));

        if (request.getTitle() != null) {
            notification.setTitle(request.getTitle());
        }
        if (request.getMessage() != null) {
            notification.setMessage(request.getMessage());
        }
        if (request.getActionUrl() != null) {
            notification.setActionUrl(request.getActionUrl());
        }
        if (request.getIconUrl() != null) {
            notification.setIconUrl(request.getIconUrl());
        }

        if (request.getScope() != null) {
            notification.setScope(request.getScope());

            if (request.getScope() == NotificationScope.INTERNAL) {
                if (request.getTargetRole() != null) {
                    notification.setTargetRole(request.getTargetRole());
                }
                notification.setTargetBuildingId(null);
            } else if (request.getScope() == NotificationScope.EXTERNAL) {
                notification.setTargetRole(null);
                if (request.getTargetBuildingId() != null) {
                    notification.setTargetBuildingId(request.getTargetBuildingId());
                }
            }

            validateNotificationScope(notification.getScope(), notification.getTargetRole(), notification.getTargetBuildingId());
        } else {
            if (request.getTargetRole() != null) {
                notification.setTargetRole(request.getTargetRole());
            }
            if (request.getTargetBuildingId() != null) {
                notification.setTargetBuildingId(request.getTargetBuildingId());
            }
        }

        Notification updatedNotification = notificationRepository.save(notification);

        // Chỉ gửi realtime notification và FCM push khi:
        // 1. scope = EXTERNAL (riêng tư cho cư dân)
        // 2. createdAt <= now (realtime)
        if (shouldSendNotification(updatedNotification)) {
            sendWebSocketNotification(updatedNotification, "NOTIFICATION_UPDATED");
            notificationPushService.sendPushNotification(updatedNotification);
            log.info("✅ [NotificationService] Sent realtime and FCM push notification for updated notification {} (EXTERNAL, createdAt <= now)", updatedNotification.getId());
        } else {
            log.info("⏭️ [NotificationService] Skipped sending notification for updated notification {} (scope={}, createdAt={})", 
                    updatedNotification.getId(), updatedNotification.getScope(), updatedNotification.getCreatedAt());
        }

        return toResponse(updatedNotification);
    }

    public void deleteNotification(UUID id, UUID userId) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found with ID: " + id));

        if (notification.isDeleted()) {
            throw new IllegalArgumentException("Notification already deleted");
        }

        notification.setDeletedAt(Instant.now());
        notification.setDeletedBy(userId);
        notificationRepository.save(notification);

        sendWebSocketNotification(notification, "NOTIFICATION_DELETED");
    }

    public List<NotificationResponse> getAllNotifications() {
        List<Notification> notifications = notificationRepository.findAllActive();
        
        return notifications.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public NotificationResponse getNotificationById(UUID id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found with ID: " + id));
        
        if (notification.isDeleted()) {
            throw new IllegalArgumentException("Notification not found with ID: " + id);
        }
        
        return toResponse(notification);
    }

    public NotificationDetailResponse getNotificationDetailById(UUID id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found with ID: " + id));
        
        if (notification.isDeleted()) {
            throw new IllegalArgumentException("Notification not found with ID: " + id);
        }
        
        return toDetailResponse(notification);
    }

    public List<NotificationResponse> getNotificationsForResident(UUID residentId, UUID buildingId) {
        NotificationPagedResponse pagedResponse = getNotificationsForResidentPaged(residentId, buildingId, 0, 7);
        return pagedResponse.getContent();
    }

    public NotificationPagedResponse getNotificationsForResidentPaged(UUID residentId, UUID buildingId, int page, int size) {
        // OPTIMIZED: Filter and paginate at database level using Spring Data JPA Pageable
        // This dramatically improves performance - only loads the requested page from database
        // Instead of loading ALL notifications into memory and filtering/sorting/paginating there
        
        // Ensure valid page number
        if (page < 0) {
            page = 0;
        }
        
        // Create Pageable with sorting by createdAt DESC (newest first)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        // Query with pagination at database level (uses LIMIT/OFFSET)
        Page<Notification> notificationPage = notificationRepository.findNotificationsForResidentOptimized(
                NotificationScope.EXTERNAL, residentId, buildingId, pageable
        );
        
        // Convert to response DTOs
        List<NotificationResponse> pagedContent = notificationPage.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return NotificationPagedResponse.builder()
                .content(pagedContent)
                .currentPage(notificationPage.getNumber())
                .pageSize(notificationPage.getSize())
                .totalElements(notificationPage.getTotalElements())
                .totalPages(notificationPage.getTotalPages())
                .hasNext(notificationPage.hasNext())
                .hasPrevious(notificationPage.hasPrevious())
                .isFirst(notificationPage.isFirst())
                .isLast(notificationPage.isLast())
                .build();
    }

    /**
     * Get total count of notifications for resident (all pages, not just current page)
     * This is used for displaying unread count on home screen
     * OPTIMIZED: Count at database level instead of loading all into memory
     */
    public long getNotificationsCountForResident(UUID residentId, UUID buildingId) {
        return notificationRepository.countNotificationsForResidentOptimized(
                NotificationScope.EXTERNAL, residentId, buildingId
        );
    }

    public List<NotificationResponse> getNotificationsForRole(String role, UUID userId) {
        List<Notification> notifications = notificationRepository.findByScopeAndRole(
                NotificationScope.INTERNAL,
                role
        );

        return notifications.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Kiểm tra xem có nên gửi notification (realtime + FCM push) không.
     * Chỉ gửi khi:
     * 1. scope = EXTERNAL (riêng tư cho cư dân)
     * 2. createdAt <= now (realtime) - chỉ gửi khi tạo notification ở thời điểm hiện tại hoặc quá khứ
     */
    private boolean shouldSendNotification(Notification notification) {
        // Chỉ gửi cho notification có scope EXTERNAL (cho cư dân)
        if (notification.getScope() != NotificationScope.EXTERNAL) {
            return false;
        }
        
        Instant now = Instant.now();
        Instant createdAt = notification.getCreatedAt();
        
        if (createdAt == null) {
            return false;
        }
        
        // Chỉ gửi khi createdAt <= now (realtime)
        // Nếu createdAt > now (không thể xảy ra trong thực tế, nhưng check để an toàn), không gửi
        if (createdAt.isAfter(now)) {
            return false; // createdAt là tương lai, không gửi notification
        }
        
        return true; // createdAt <= now, gửi notification (realtime)
    }

    private void validateNotificationScope(NotificationScope scope, String targetRole, UUID targetBuildingId) {
        if (scope == null) {
            throw new IllegalArgumentException("Scope is required");
        }

        if (scope == NotificationScope.INTERNAL) {
            if (targetRole == null || targetRole.isBlank()) {
                throw new IllegalArgumentException("INTERNAL notification must have target_role (use 'ALL' for all roles)");
            }
            if (targetBuildingId != null) {
                throw new IllegalArgumentException("INTERNAL notification cannot have target_building_id");
            }
        } else if (scope == NotificationScope.EXTERNAL) {
            if (targetRole != null && !targetRole.isBlank()) {
                throw new IllegalArgumentException("EXTERNAL notification cannot have target_role");
            }
        }
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .scope(notification.getScope())
                .targetRole(notification.getTargetRole())
                .targetBuildingId(notification.getTargetBuildingId())
                .referenceId(notification.getReferenceId())
                .referenceType(notification.getReferenceType())
                .actionUrl(notification.getActionUrl())
                .iconUrl(notification.getIconUrl())
                .createdAt(notification.getCreatedAt())
                .updatedAt(notification.getUpdatedAt())
                .build();
    }

    private boolean shouldShowNotificationToResident(Notification notification, UUID residentId, UUID buildingId) {
        if (notification.getScope() == NotificationScope.INTERNAL) {
            return false;
        }

        if (notification.getScope() == NotificationScope.EXTERNAL) {
            NotificationType type = notification.getType();
            
            // Card-related notifications (CARD_FEE_REMINDER, CARD_APPROVED, CARD_REJECTED):
            // RIÊNG TƯ - chỉ hiển thị cho resident tạo thẻ
            // These notifications must have targetResidentId set
            if (type == NotificationType.CARD_FEE_REMINDER || 
                type == NotificationType.CARD_APPROVED || 
                type == NotificationType.CARD_REJECTED) {
                // Card notifications must have targetResidentId and match current resident
                if (notification.getTargetResidentId() == null) {
                    // This is likely an old notification created before targetResidentId was required
                    // Log at debug level to avoid noise in logs
                    log.debug("⚠️ [NotificationService] Card notification {} missing targetResidentId (likely old notification), skipping", notification.getId());
                    return false; // Don't show card notifications without targetResidentId
                }
                return residentId != null && residentId.equals(notification.getTargetResidentId());
            }
            
            // For other notification types, use existing logic
            // If notification has targetResidentId, only show to that specific resident
            if (notification.getTargetResidentId() != null) {
                return residentId != null && residentId.equals(notification.getTargetResidentId());
            }
            
            // Otherwise, use building-based filtering (for notifications to all residents in building or all buildings)
            if (notification.getTargetBuildingId() == null) {
                return true; // Show to all buildings
            }
            return buildingId != null && buildingId.equals(notification.getTargetBuildingId());
        }

        return false;
    }

    /**
     * Get residentId from userId by calling base-service
     * This ensures that notifications are filtered by the authenticated user's residentId
     */
    @Transactional(readOnly = true)
    public UUID getResidentIdFromUserId(UUID userId) {
        try {
            return baseServiceClient.getResidentIdByUserId(userId);
        } catch (Exception e) {
            log.error("❌ Error getting residentId from userId {}: {}", userId, e.getMessage(), e);
            return null;
        }
    }

    private NotificationDetailResponse toDetailResponse(Notification notification) {
        return NotificationDetailResponse.builder()
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .scope(notification.getScope())
                .targetBuildingId(notification.getTargetBuildingId())
                .targetResidentId(notification.getTargetResidentId())
                .actionUrl(notification.getActionUrl())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    private void sendWebSocketNotification(Notification notification, String action) {
        try {
            NotificationWebSocketMessage payload = NotificationWebSocketMessage.of(notification, action);
            
            // If notification has targetResidentId, only send to that specific resident
            // Don't broadcast to building/external channels to prevent other residents from receiving it
            if (notification.getTargetResidentId() != null) {
                String residentDestination = "/topic/notifications/resident/" + notification.getTargetResidentId();
                messagingTemplate.convertAndSend(residentDestination, payload);
                return;
            }

            // Gửi kênh tổng cho tất cả client quan tâm (only for non-resident-specific notifications)
            messagingTemplate.convertAndSend("/topic/notifications", payload);

            if (notification.getScope() == NotificationScope.EXTERNAL) {
                if (notification.getTargetBuildingId() != null) {
                    String destination = "/topic/notifications/building/" + notification.getTargetBuildingId();
                    messagingTemplate.convertAndSend(destination, payload);
                } else {
                    messagingTemplate.convertAndSend("/topic/notifications/external", payload);
                }
            } else if (notification.getScope() == NotificationScope.INTERNAL) {
                if (notification.getTargetRole() != null && !notification.getTargetRole().isBlank()) {
                    String destination = "/topic/notifications/role/" + notification.getTargetRole();
                    messagingTemplate.convertAndSend(destination, payload);
                } else {
                    messagingTemplate.convertAndSend("/topic/notifications/internal", payload);
                }
            }
        } catch (Exception e) {
            log.error("❌ Error sending WebSocket notification", e);
        }
    }
    
    private void sendWebSocketNotificationToResident(Notification notification, UUID residentId, String action) {
        NotificationWebSocketMessage payload = NotificationWebSocketMessage.of(notification, action);
        try {
            // Send to resident-specific channel first (most specific)
            String residentDestination = "/topic/notifications/resident/" + residentId;
            messagingTemplate.convertAndSend(residentDestination, payload);
            log.info("🔔 WebSocket {} | Destination: {} | Notification ID: {} | ResidentId: {}", 
                    action, residentDestination, notification.getId(), residentId);
            
            // Also send to building channel if applicable (for backward compatibility)
            if (notification.getScope() == NotificationScope.EXTERNAL && notification.getTargetBuildingId() != null) {
                String buildingDestination = "/topic/notifications/building/" + notification.getTargetBuildingId();
                messagingTemplate.convertAndSend(buildingDestination, payload);
                log.info("🔔 WebSocket {} | Destination: {} | Notification ID: {}", action, buildingDestination, notification.getId());
            }
            
            log.info("✅ Notification sent successfully via WebSocket to resident {}", residentId);
        } catch (Exception e) {
            log.error("❌ Error sending WebSocket notification to resident {}", residentId, e);
        }
    }

    public void createInternalNotification(com.QhomeBase.customerinteractionservice.dto.notification.InternalNotificationRequest request) {
        log.info("📥 [NotificationService] ========== RECEIVED INTERNAL NOTIFICATION REQUEST ==========");
        log.info("📥 [NotificationService] Type: {}", request.getType());
        log.info("📥 [NotificationService] ResidentId: {}", request.getResidentId());
        log.info("📥 [NotificationService] BuildingId: {}", request.getBuildingId());
        log.info("📥 [NotificationService] Title: {}", request.getTitle());
        log.info("📥 [NotificationService] Message: {}", request.getMessage());
        log.info("📥 [NotificationService] ReferenceType: {}", request.getReferenceType());
        log.info("📥 [NotificationService] ReferenceId: {}", request.getReferenceId());
        log.info("📥 [NotificationService] Data: {}", request.getData());
        
        NotificationType type = request.getType();
        
        // Validate: All card-related notifications require residentId (private notifications)
        if (type == NotificationType.CARD_FEE_REMINDER || 
            type == NotificationType.CARD_APPROVED || 
            type == NotificationType.CARD_REJECTED) {
            if (request.getResidentId() == null) {
                log.error("❌ [NotificationService] ========== VALIDATION FAILED ==========");
                log.error("❌ [NotificationService] Card notification (type={}) requires residentId, but it's null", type);
                throw new IllegalArgumentException("Card notifications must have residentId");
            }
            log.info("✅ [NotificationService] Validation passed: Card notification has residentId: {}", request.getResidentId());
        }
        
            // If residentId is provided, send directly to that resident (private notification)
        if (request.getResidentId() != null) {
            log.info("📤 [NotificationService] ========== PROCESSING PRIVATE NOTIFICATION ==========");
            log.info("📤 [NotificationService] ResidentId: {}", request.getResidentId());
            log.info("📤 [NotificationService] Type: {}", request.getType());
            log.info("📤 [NotificationService] ReferenceType: {}", request.getReferenceType());
            // Check if notification already exists for this referenceId, type, and residentId
            // This prevents duplicate FCM push and WebSocket notifications when admin approves/denies the same request multiple times
            boolean shouldSendNotifications = true;
            if (request.getReferenceId() != null && request.getType() != null) {
                log.info("🔍 [NotificationService] Checking for duplicate notification...");
                log.info("🔍 [NotificationService] ReferenceId: {}", request.getReferenceId());
                log.info("🔍 [NotificationService] Type: {}", request.getType());
                log.info("🔍 [NotificationService] ResidentId: {}", request.getResidentId());
                
                List<com.QhomeBase.customerinteractionservice.model.Notification> existingNotifications = 
                        notificationRepository.findByReferenceIdAndTypeAndTargetResidentId(
                                request.getReferenceId(),
                                request.getType(),
                                request.getResidentId()
                        );
                
                log.info("🔍 [NotificationService] Found {} existing notification(s)", existingNotifications.size());
                
                if (!existingNotifications.isEmpty()) {
                    log.warn("⚠️ [NotificationService] ========== DUPLICATE DETECTED ==========");
                    log.warn("⚠️ [NotificationService] Notification already exists for referenceId={}, type={}, residentId={}", 
                            request.getReferenceId(), request.getType(), request.getResidentId());
                    log.warn("⚠️ [NotificationService] Existing notification ID: {}", existingNotifications.get(0).getId());
                    log.warn("⚠️ [NotificationService] Skipping FCM push and WebSocket to avoid duplicate");
                    // Skip both FCM push and WebSocket notification
                    shouldSendNotifications = false;
                } else {
                    log.info("✅ [NotificationService] No duplicate found - safe to send notifications");
                }
            }
            
            if (shouldSendNotifications) {
                log.info("📤 [NotificationService] ========== SENDING FCM PUSH ==========");
                // No existing notification - safe to send FCM push and WebSocket
                Map<String, String> dataPayload = new HashMap<>();
                dataPayload.put("type", request.getType() != null ? request.getType().name() : "SYSTEM");
                if (request.getReferenceId() != null) {
                    dataPayload.put("referenceId", request.getReferenceId().toString());
                }
                if (request.getReferenceType() != null) {
                    dataPayload.put("referenceType", request.getReferenceType());
                }
                if (request.getData() != null) {
                    dataPayload.putAll(request.getData());
                }
                
                log.info("📤 [NotificationService] FCM Payload - ResidentId: {}", request.getResidentId());
                log.info("📤 [NotificationService] FCM Payload - Title: {}", request.getTitle());
                log.info("📤 [NotificationService] FCM Payload - Message: {}", request.getMessage());
                log.info("📤 [NotificationService] FCM Payload - Data: {}", dataPayload);

                // Send push notification directly to resident (only if no existing notification)
                notificationPushService.sendPushNotificationToResident(
                        request.getResidentId(),
                        request.getTitle(),
                        request.getMessage(),
                        dataPayload
                );
                log.info("✅ [NotificationService] FCM push notification sent successfully");
                log.info("✅ [NotificationService] ReferenceId: {}, Type: {}, ResidentId: {}", 
                        request.getReferenceId(), request.getType(), request.getResidentId());
            } else {
                log.warn("⏭️ [NotificationService] Skipped FCM push (duplicate detected)");
            }

            // Also save to DB with scope EXTERNAL and targetResidentId for specific resident
            // IMPORTANT: When residentId is provided, always set targetResidentId and targetBuildingId = null
            // to ensure only the resident who created the request sees the notification (PRIVATE notification)
            // This applies to all request types: card registrations, cleaning requests, maintenance requests, etc.
            NotificationScope scope = NotificationScope.EXTERNAL;
            
            Notification notification = Notification.builder()
                    .type(request.getType())
                    .title(request.getTitle())
                    .message(request.getMessage())
                    .scope(scope)
                    .targetResidentId(request.getResidentId()) // Set targetResidentId for specific resident (PRIVATE)
                    .targetBuildingId(null) // Always null when targeting specific resident (ensures PRIVATE notification)
                    .targetRole(null)
                    .referenceId(request.getReferenceId())
                    .referenceType(request.getReferenceType())
                    .actionUrl(request.getActionUrl())
                    .iconUrl(request.getIconUrl())
                    .build();

            log.info("💾 [NotificationService] ========== SAVING TO DATABASE ==========");
            log.info("💾 [NotificationService] Notification details:");
            log.info("💾 [NotificationService]   - Type: {}", notification.getType());
            log.info("💾 [NotificationService]   - Title: {}", notification.getTitle());
            log.info("💾 [NotificationService]   - Message: {}", notification.getMessage());
            log.info("💾 [NotificationService]   - Scope: {}", notification.getScope());
            log.info("💾 [NotificationService]   - TargetResidentId: {}", notification.getTargetResidentId());
            log.info("💾 [NotificationService]   - TargetBuildingId: {}", notification.getTargetBuildingId());
            log.info("💾 [NotificationService]   - ReferenceId: {}", notification.getReferenceId());
            log.info("💾 [NotificationService]   - ReferenceType: {}", notification.getReferenceType());
            
            Notification savedNotification = notificationRepository.save(notification);
            log.info("✅ [NotificationService] Notification saved successfully");
            log.info("✅ [NotificationService] Notification ID: {}", savedNotification.getId());
            log.info("✅ [NotificationService] ResidentId: {}", request.getResidentId());
            log.info("✅ [NotificationService] Type: {}", request.getType());
            log.info("✅ [NotificationService] ReferenceType: {}", request.getReferenceType());
            
            // Send WebSocket notification ONLY if we also sent FCM push (i.e., no duplicate)
            // This ensures both FCM and WebSocket are sent together, or both are skipped together
            if (shouldSendNotifications) {
                log.info("🔔 [NotificationService] ========== SENDING WEBSOCKET NOTIFICATION ==========");
                log.info("🔔 [NotificationService] Notification ID: {}", savedNotification.getId());
                log.info("🔔 [NotificationService] TargetResidentId: {}", savedNotification.getTargetResidentId());
                log.info("🔔 [NotificationService] WebSocket destination: /topic/notifications/resident/{}", savedNotification.getTargetResidentId());
                
                // Send WebSocket notification - will automatically route to resident-specific channel
                // since targetResidentId is set, it won't broadcast to building/external channels
                sendWebSocketNotification(savedNotification, "NOTIFICATION_CREATED");
                log.info("✅ [NotificationService] WebSocket notification sent successfully");
                log.info("✅ [NotificationService] Notification ID: {} | ResidentId: {} | Type: {}", 
                        savedNotification.getId(), request.getResidentId(), request.getType());
            } else {
                log.warn("⏭️ [NotificationService] Skipped WebSocket notification (duplicate detected)");
                log.warn("⏭️ [NotificationService] Notification ID: {} | ResidentId: {} | Type: {}", 
                        savedNotification.getId(), request.getResidentId(), request.getType());
            }
            log.info("📥 [NotificationService] ========== INTERNAL NOTIFICATION PROCESSING COMPLETE ==========");
        } else {
            log.warn("⚠️ [NotificationService] No residentId provided, using fallback notification creation | Type: {} | BuildingId: {}", 
                    request.getType(), request.getBuildingId());
            // Fallback to regular notification creation
            CreateNotificationRequest createRequest = CreateNotificationRequest.builder()
                    .type(request.getType())
                    .title(request.getTitle())
                    .message(request.getMessage())
                    .scope(request.getTargetRole() != null 
                            ? NotificationScope.INTERNAL 
                            : NotificationScope.EXTERNAL)
                    .targetBuildingId(request.getBuildingId())
                    .targetRole(request.getTargetRole())
                    .referenceId(request.getReferenceId())
                    .referenceType(request.getReferenceType())
                    .actionUrl(request.getActionUrl())
                    .iconUrl(request.getIconUrl())
                    .build();
            
            createNotification(createRequest);
        }
    }

    /**
     * Send FCM push notification only (without saving to DB)
     * Used for chat messages and other real-time notifications that should not appear in notification list
     */
    public void sendPushOnly(UUID residentId, String title, String body, Map<String, String> dataPayload) {
        notificationPushService.sendPushNotificationToResident(residentId, title, body, dataPayload);
        log.debug("Sent push-only notification to resident: {} (not saved to DB)", residentId);
    }
}
