package com.QhomeBase.marketplaceservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.UUID;

/**
 * Service to fetch blocked users from chat-service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceClient {

    @Value("${chat.service.url:http://localhost:8090}")
    private String chatServiceUrl;

    private WebClient webClient = WebClient.builder().build();

    /**
     * Get list of blocked user IDs (residentIds) from chat-service
     * Returns empty list if error occurs
     */
    public List<UUID> getBlockedUserIds(String accessToken) {
        return getBlockedUserIds(accessToken, "/api/direct-chat/blocked-users");
    }

    /**
     * Get list of users who blocked current user (residentIds) from chat-service
     * Returns empty list if error occurs
     */
    public List<UUID> getBlockedByUserIds(String accessToken) {
        return getBlockedUserIds(accessToken, "/api/direct-chat/blocked-by-users");
    }

    /**
     * Internal method to get blocked user IDs from a specific endpoint
     */
    private List<UUID> getBlockedUserIds(String accessToken, String endpoint) {
        if (accessToken == null || accessToken.isEmpty()) {
            log.warn("No access token provided for getting blocked users");
            return List.of();
        }

        try {
            log.info("🔍 [ChatServiceClient] Fetching blocked users from chat-service: {}", endpoint);
            
            String url = chatServiceUrl + endpoint;
            log.info("🔍 [ChatServiceClient] Calling URL: {}", url);
            
            List<UUID> blockedUserIds = webClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<UUID>>() {})
                    .block();
            
            if (blockedUserIds == null) {
                log.warn("⚠️ [ChatServiceClient] Blocked users response is null");
                return List.of();
            }
            
            log.info("✅ [ChatServiceClient] Found {} blocked users from {}", blockedUserIds.size(), endpoint);
            if (!blockedUserIds.isEmpty()) {
                log.info("🔍 [ChatServiceClient] Blocked user IDs: {}", blockedUserIds);
            }
            return blockedUserIds;
            
        } catch (WebClientResponseException.Unauthorized e) {
            log.warn("Unauthorized when fetching blocked users - token may be invalid");
            return List.of();
        } catch (WebClientResponseException e) {
            log.warn("Error fetching blocked users - Status: {}, Body: {}", 
                    e.getStatusCode(), e.getResponseBodyAsString());
            return List.of();
        } catch (Exception e) {
            log.error("Error fetching blocked users: {}", e.getMessage(), e);
            return List.of();
        }
    }
}

