package com.QhomeBase.chatservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import com.QhomeBase.chatservice.security.UserPrincipal;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResidentInfoService {

    @Value("${base.service.url:http://localhost:8081}")
    private String baseServiceUrl;

    private final WebClient webClient = WebClient.builder().build();

    private String getCurrentAccessToken() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
                UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
                String token = principal.token();
                if (token != null && !token.isEmpty()) {
                    return token;
                }
            }
        } catch (Exception e) {
            log.debug("Could not get token from SecurityContext: {}", e.getMessage());
        }
        
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return null;
            }
            
            HttpServletRequest request = attributes.getRequest();
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return null;
            }
            
            return auth.substring(7);
        } catch (Exception e) {
            log.error("Error getting access token from request: {}", e.getMessage(), e);
            return null;
        }
    }

    public Map<String, Object> getResidentInfo(UUID residentId) {
        if (residentId == null) {
            return null;
        }

        String accessToken = getCurrentAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            log.warn("No access token available for fetching resident info: {}", residentId);
            return null;
        }

        try {
            String url = baseServiceUrl + "/api/residents/" + residentId;
            
            return webClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (Exception e) {
            log.error("Error fetching resident info for residentId: {}", residentId, e);
            return null;
        }
    }

    public UUID getResidentIdFromUserId(UUID userId, String accessToken) {
        if (userId == null) {
            return null;
        }
        
        try {
            String url = baseServiceUrl + "/api/residents/by-user/" + userId;
            
            Map<String, Object> response = webClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            
            if (response == null) {
                return null;
            }
            
            Object idObj = response.get("id");
            if (idObj == null) {
                return null;
            }
            
            return UUID.fromString(idObj.toString());
        } catch (WebClientResponseException.NotFound e) {
            // 404 means userId not found - this is expected if inviteeId is already a residentId
            // Log as debug instead of error to reduce noise
            log.debug("UserId {} not found in base-service (404), assuming it's already a residentId", userId);
            return null;
        } catch (Exception e) {
            // For other errors, log as error
            log.error("Error getting residentId for userId: {}", userId, e);
            return null;
        }
    }

    /**
     * Get resident name by residentId
     */
    public String getResidentName(UUID residentId, String accessToken) {
        if (residentId == null) {
            return "Unknown";
        }

        Map<String, Object> residentInfo = getResidentInfo(residentId);
        if (residentInfo == null) {
            return "Unknown";
        }

        // Try to get name from various possible fields
        Object nameObj = residentInfo.get("name");
        if (nameObj != null) {
            return nameObj.toString();
        }

        Object fullNameObj = residentInfo.get("fullName");
        if (fullNameObj != null) {
            return fullNameObj.toString();
        }

        Object firstNameObj = residentInfo.get("firstName");
        Object lastNameObj = residentInfo.get("lastName");
        if (firstNameObj != null || lastNameObj != null) {
            String firstName = firstNameObj != null ? firstNameObj.toString() : "";
            String lastName = lastNameObj != null ? lastNameObj.toString() : "";
            return (firstName + " " + lastName).trim();
        }

        return "Unknown";
    }

    /**
     * Get residentId from userId (using current access token)
     */
    public UUID getResidentIdFromUserId(UUID userId) {
        String accessToken = getCurrentAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            log.warn("No access token available for getting residentId from userId: {}", userId);
            return null;
        }
        return getResidentIdFromUserId(userId, accessToken);
    }

    /**
     * Get userId from residentId
     */
    public UUID getUserIdFromResidentId(UUID residentId) {
        if (residentId == null) {
            return null;
        }
        
        String accessToken = getCurrentAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            log.warn("No access token available for getting userId from residentId: {}", residentId);
            return null;
        }
        
        try {
            String url = baseServiceUrl + "/api/residents/" + residentId;
            
            Map<String, Object> response = webClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            
            if (response == null) {
                return null;
            }
            
            Object userIdObj = response.get("userId");
            if (userIdObj == null) {
                return null;
            }
            
            return UUID.fromString(userIdObj.toString());
        } catch (Exception e) {
            log.error("Error getting userId for residentId: {}", residentId, e);
            return null;
        }
    }
}

