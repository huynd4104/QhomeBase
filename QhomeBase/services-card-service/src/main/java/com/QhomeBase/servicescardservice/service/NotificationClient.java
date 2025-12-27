package com.QhomeBase.servicescardservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${services.notification.base-url:http://localhost:8086}")
    private String notificationServiceBaseUrl;

    public void sendNotification(Map<String, Object> payload) {
        try {
            URI uri = UriComponentsBuilder
                    .fromUriString(notificationServiceBaseUrl)
                    .path("/api/notifications/internal")
                    .build()
                    .toUri();

            log.info("📤 [NotificationClient] ========== HTTP REQUEST ==========");
            log.info("📤 [NotificationClient] URL: {}", uri);
            log.info("📤 [NotificationClient] Method: POST");
            log.info("📤 [NotificationClient] Payload size: {} keys", payload.size());
            log.info("📤 [NotificationClient] Payload: {}", payload);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<Void> response = restTemplate.exchange(
                    uri,
                    HttpMethod.POST,
                    new HttpEntity<>(payload, headers),
                    Void.class
            );

            log.info("📤 [NotificationClient] ========== HTTP RESPONSE ==========");
            log.info("📤 [NotificationClient] Status Code: {}", response.getStatusCode());
            log.info("📤 [NotificationClient] Status Value: {}", response.getStatusCode().value());

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("❌ [NotificationClient] FAILED to push notification: status={}", response.getStatusCode());
            } else {
                log.info("✅ [NotificationClient] SUCCESS - Notification sent to notification service");
                log.info("✅ [NotificationClient] Next steps: NotificationService will process and send FCM + WebSocket");
            }
        } catch (Exception ex) {
            log.error("❌ [NotificationClient] ========== EXCEPTION ==========");
            log.error("❌ [NotificationClient] Error sending notification to notification service", ex);
            log.error("❌ [NotificationClient] Exception type: {}", ex.getClass().getName());
            log.error("❌ [NotificationClient] Exception message: {}", ex.getMessage());
            if (ex.getCause() != null) {
                log.error("❌ [NotificationClient] Caused by: {}", ex.getCause().getMessage());
            }
            // Re-throw để caller biết có lỗi (optional, tùy vào yêu cầu)
            // throw new RuntimeException("Failed to send notification", ex);
        }
    }

    public void sendResidentNotification(UUID residentId,
                                         UUID buildingId,
                                         String type,
                                         String title,
                                         String message,
                                         UUID referenceId,
                                         String referenceType,
                                         Map<String, String> data) {
        // For public notifications (CARD_APPROVED, CARD_REJECTED), residentId can be null
        // but buildingId must be provided
        if (residentId == null && buildingId == null) {
            log.warn("⚠️ [NotificationClient] Both residentId and buildingId are null, skip push");
            return;
        }
        
        log.info("📨 [NotificationClient] ========== PREPARING NOTIFICATION ==========");
        log.info("📨 [NotificationClient] Type: {}", type);
        log.info("📨 [NotificationClient] Title: {}", title);
        log.info("📨 [NotificationClient] Message: {}", message);
        log.info("📨 [NotificationClient] ResidentId: {}", residentId);
        log.info("📨 [NotificationClient] BuildingId: {}", buildingId);
        log.info("📨 [NotificationClient] ReferenceId: {}", referenceId);
        log.info("📨 [NotificationClient] ReferenceType: {}", referenceType);
        log.info("📨 [NotificationClient] Data: {}", data);
        
        Map<String, Object> payload = new HashMap<>();
        if (residentId != null) {
            payload.put("residentId", residentId.toString());
            log.info("✅ [NotificationClient] Added residentId to payload: {}", residentId);
        } else {
            log.warn("⚠️ [NotificationClient] residentId is NULL - notification may not be delivered!");
        }
        if (buildingId != null) {
            payload.put("buildingId", buildingId.toString());
            log.info("✅ [NotificationClient] Added buildingId to payload: {}", buildingId);
        } else {
            log.info("ℹ️ [NotificationClient] buildingId is null (expected for private notifications)");
        }
        payload.put("type", type != null ? type : "SYSTEM");
        payload.put("title", title);
        payload.put("message", message);
        if (referenceId != null) {
            payload.put("referenceId", referenceId.toString());
        }
        if (referenceType != null) {
            payload.put("referenceType", referenceType);
        }
        if (data != null && !data.isEmpty()) {
            payload.put("data", data);
        }
        
        log.info("📤 [NotificationClient] ========== SENDING NOTIFICATION ==========");
        log.info("📤 [NotificationClient] Full payload: {}", payload);
        log.info("📤 [NotificationClient] Target: notificationServiceBaseUrl={}", notificationServiceBaseUrl);
        log.info("📤 [NotificationClient] Endpoint: /api/notifications/internal");
        log.info("📤 [NotificationClient] This will trigger FCM push + WebSocket realtime for residentId: {}", residentId);
        sendNotification(payload);
        log.info("📤 [NotificationClient] ========== NOTIFICATION SENT ==========");
    }
}

