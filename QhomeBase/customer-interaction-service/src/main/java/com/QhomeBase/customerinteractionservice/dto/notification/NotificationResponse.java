package com.QhomeBase.customerinteractionservice.dto.notification;

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
public class NotificationResponse {

    private UUID id;

    private NotificationType type;

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

    private Instant updatedAt;
}

