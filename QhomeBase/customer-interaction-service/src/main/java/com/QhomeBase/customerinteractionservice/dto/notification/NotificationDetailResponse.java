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
public class NotificationDetailResponse {

    private NotificationType type;

    private String title;

    private String message;

    private NotificationScope scope;

    private UUID targetBuildingId;

    private UUID targetResidentId;

    private String actionUrl;

    private Instant createdAt;
}

