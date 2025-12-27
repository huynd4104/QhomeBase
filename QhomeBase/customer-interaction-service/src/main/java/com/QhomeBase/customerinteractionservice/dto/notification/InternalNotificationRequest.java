package com.QhomeBase.customerinteractionservice.dto.notification;

import com.QhomeBase.customerinteractionservice.model.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalNotificationRequest {
    
    @NotNull(message = "Type is required")
    private NotificationType type;
    
    @NotBlank(message = "Title is required")
    private String title;
    
    @NotBlank(message = "Message is required")
    private String message;
    
    // Target: can be residentId, buildingId, or role
    private UUID residentId;
    private UUID buildingId;
    private String targetRole;
    
    private UUID referenceId;
    private String referenceType;
    
    private String actionUrl;
    private String iconUrl;
    
    private Map<String, String> data;
}

