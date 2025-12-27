package com.QhomeBase.marketplaceservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Client to send push notifications via customer-interaction-service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationClient {

    @Value("${customer-interaction.service.url:http://localhost:8086}")
    private String customerInteractionServiceUrl;

    private WebClient webClient = WebClient.builder().build();

    /**
     * Send FCM push notification to a resident
     * This calls customer-interaction-service to send the push notification
     */
    public void sendPushNotificationToResident(UUID residentId, String title, String body, Map<String, String> dataPayload) {
        if (residentId == null) {
            log.warn("⚠️ [NotificationClient] residentId is null, skip push notification");
            return;
        }

        try {
            log.info("🔔 [NotificationClient] Sending push notification to residentId: {}, title: {}", residentId, title);
            
            String url = customerInteractionServiceUrl + "/api/notifications/push-only";
            log.debug("🔔 [NotificationClient] Calling URL: {}", url);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("residentId", residentId.toString());
            requestBody.put("title", title);
            requestBody.put("message", body);
            if (dataPayload != null) {
                requestBody.put("data", dataPayload);
            }
            
            webClient
                    .post()
                    .uri(url)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
            
            log.info("✅ [NotificationClient] Push notification sent successfully to residentId: {}", residentId);
            
        } catch (WebClientResponseException e) {
            log.error("❌ [NotificationClient] Error sending push notification to residentId {} - Status: {}, Body: {}", 
                    residentId, e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("❌ [NotificationClient] Error sending push notification to residentId {}: {}", residentId, e.getMessage(), e);
        }
    }
}
