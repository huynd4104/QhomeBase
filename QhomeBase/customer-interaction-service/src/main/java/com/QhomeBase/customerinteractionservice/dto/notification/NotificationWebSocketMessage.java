package com.QhomeBase.customerinteractionservice.dto.notification;

import com.QhomeBase.customerinteractionservice.model.Notification;
import com.QhomeBase.customerinteractionservice.model.NotificationScope;
import com.QhomeBase.customerinteractionservice.model.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationWebSocketMessage {

    private String eventType;
    private UUID notificationId;
    private NotificationType notificationType;
    private String title;
    private String message;
    private NotificationScope scope;
    private String targetRole;
    private UUID targetBuildingId;
    private UUID referenceId;
    private String referenceType;
    private String actionUrl;
    private String iconUrl;
    private Instant createdAt;
    private Boolean isRead;
    private Instant readAt;
    private UUID targetResidentId;

    public static NotificationWebSocketMessage of(Notification notification, String eventType) {
        // For new notifications, isRead is always false (unread)
        // Read status is tracked client-side in Flutter via NotificationReadStore
        return NotificationWebSocketMessage.builder()
                .eventType(eventType)
                .notificationId(notification.getId())
                .notificationType(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .scope(notification.getScope())
                .targetRole(notification.getTargetRole())
                .targetBuildingId(notification.getTargetBuildingId())
                .referenceId(notification.getReferenceId())
                .referenceType(notification.getReferenceType())
                .actionUrl(notification.getActionUrl())
                .iconUrl(notification.getIconUrl())
                .createdAt(notification.getCreatedAt() != null ? notification.getCreatedAt() : Instant.now())
                .isRead(false) // New notifications are always unread
                .readAt(null) // New notifications don't have readAt
                .targetResidentId(notification.getTargetResidentId())
                .build();
    }
}


