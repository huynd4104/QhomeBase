package com.QhomeBase.baseservice.service;

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

    @SuppressWarnings("null")
    public void sendNotification(Map<String, Object> payload) {
        try {
            URI uri = UriComponentsBuilder
                    .fromUriString(notificationServiceBaseUrl)
                    .path("/api/notifications/internal")
                    .build()
                    .toUri();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            log.info("📤 [NotificationClient] Sending notification to {} | Payload: residentId={}, type={}, title={}, referenceType={}", 
                    uri, payload.get("residentId"), payload.get("type"), payload.get("title"), payload.get("referenceType"));

            ResponseEntity<Void> response = restTemplate.exchange(
                    uri,
                    HttpMethod.POST,
                    new HttpEntity<>(payload, headers),
                    Void.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ [NotificationClient] Successfully sent notification to notification service | ResidentId: {} | Type: {} | ReferenceType: {}", 
                        payload.get("residentId"), payload.get("type"), payload.get("referenceType"));
            } else {
                log.warn("❌ [NotificationClient] Failed to push notification to notification service | Status: {} | ResidentId: {} | Type: {} | ReferenceType: {}", 
                        response.getStatusCode(), payload.get("residentId"), payload.get("type"), payload.get("referenceType"));
            }
        } catch (Exception ex) {
            log.error("❌ [NotificationClient] Error sending notification to notification service | ResidentId: {} | Type: {} | ReferenceType: {} | Error: {}", 
                    payload.get("residentId"), payload.get("type"), payload.get("referenceType"), ex.getMessage(), ex);
        }
    }

    @SuppressWarnings("null")
    public void sendResidentNotification(UUID residentId,
                                         UUID buildingId,
                                         String type,
                                         String title,
                                         String message,
                                         UUID referenceId,
                                         String referenceType,
                                         Map<String, String> data) {
        if (residentId == null) {
            log.warn("⚠️ [NotificationClient] residentId null, skip push");
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("residentId", residentId.toString());
        if (buildingId != null) {
            payload.put("buildingId", buildingId.toString());
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

        sendNotification(payload);
    }
}

