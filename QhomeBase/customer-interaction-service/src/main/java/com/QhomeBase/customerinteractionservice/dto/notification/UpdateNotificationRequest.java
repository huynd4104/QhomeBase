package com.QhomeBase.customerinteractionservice.dto.notification;

import com.QhomeBase.customerinteractionservice.model.NotificationScope;
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
public class UpdateNotificationRequest {

    private String title;

    private String message;

    private NotificationScope scope;

    private String targetRole;

    private UUID targetBuildingId;

    private String actionUrl;

    private String iconUrl;
}

