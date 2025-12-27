package com.QhomeBase.financebillingservice.service;

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

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<Void> response = restTemplate.exchange(
                    uri,
                    HttpMethod.POST,
                    new HttpEntity<>(payload, headers),
                    Void.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("❌ [NotificationClient] Failed to push notification: status={}", response.getStatusCode());
            } else {
                log.info("✅ [NotificationClient] Notification sent successfully");
            }
        } catch (Exception ex) {
            log.error("❌ [NotificationClient] Error sending notification", ex);
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
        // Set actionUrl for invoice notifications to enable deep linking
        if (referenceType != null && (referenceType.equals("INVOICE_REMINDER") || referenceType.equals("INVOICE_UNPAID")) 
            && referenceId != null) {
            payload.put("actionUrl", "/invoices?invoiceId=" + referenceId.toString());
        }
        if (data != null && !data.isEmpty()) {
            payload.put("data", data);
        }
        
        sendNotification(payload);
    }
}

