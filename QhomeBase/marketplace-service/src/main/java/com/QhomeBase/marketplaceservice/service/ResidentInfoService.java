package com.QhomeBase.marketplaceservice.service;

import com.QhomeBase.marketplaceservice.dto.ResidentInfoResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import com.QhomeBase.marketplaceservice.security.UserPrincipal;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service to fetch resident information from base-service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResidentInfoService {

    @Value("${base.service.url:http://localhost:8081}")
    private String baseServiceUrl;

    private WebClient webClient = WebClient.builder().build();
    
    /**
     * Get residentId from userId by calling base-service /api/residents/by-user/{userId}
     */
    public UUID getResidentIdFromUserId(UUID userId, String accessToken) {
        if (userId == null) {
            return null;
        }
        
        try {
            String url = baseServiceUrl + "/api/residents/by-user/" + userId;
            
            var webClientBuilder = webClient
                    .get()
                    .uri(url);
            
            if (accessToken != null && !accessToken.isEmpty()) {
                webClientBuilder = webClientBuilder.header("Authorization", "Bearer " + accessToken);
            }
            
            Map<String, Object> response = webClientBuilder
                    .retrieve()
                    .onStatus(status -> status.value() == 404, clientResponse -> {
                        log.warn("Resident not found (404) for userId: {}", userId);
                        return Mono.error(WebClientResponseException.create(
                                HttpStatus.NOT_FOUND.value(),
                                "Resident not found for userId: " + userId,
                                clientResponse.headers().asHttpHeaders(),
                                null,
                                null
                        ));
                    })
                    .onStatus(status -> status.value() == 500, clientResponse -> {
                        log.error("Internal server error (500) when getting residentId for userId: {}", userId);
                        return clientResponse.bodyToMono(String.class)
                                .flatMap(body -> {
                                    log.error("500 Response body: {}", body);
                                    return Mono.error(WebClientResponseException.create(
                                            HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                            "Internal server error when getting resident for userId: " + userId,
                                            clientResponse.headers().asHttpHeaders(),
                                            body != null ? body.getBytes() : null,
                                            null
                                    ));
                                });
                    })
                    .bodyToMono(Map.class)
                    .block();
            
            if (response == null) {
                log.warn("Resident not found for userId: {} - response is null", userId);
                return null;
            }
            
            Object idObj = response.get("id");
            if (idObj == null) {
                log.warn("Resident response for userId {} does not contain 'id' field. Keys: {}", userId, response.keySet());
                return null;
            }
            
            UUID residentId = UUID.fromString(idObj.toString());
            return residentId;
            
        } catch (WebClientResponseException.NotFound e) {
            log.warn("Resident not found (404) for userId: {}", userId);
            return null;
        } catch (WebClientResponseException.InternalServerError e) {
            log.error("Internal server error (500) when getting residentId for userId: {}. Response body: {}", 
                    userId, e.getResponseBodyAsString());
            return null;
        } catch (WebClientResponseException e) {
            log.error("HTTP error when getting residentId for userId: {} - Status: {}, Body: {}", 
                    userId, e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.error("Error getting residentId for userId: {}", userId, e);
            return null;
        }
    }

    /**
     * Get current access token from request or SecurityContext
     */
    private String getCurrentAccessToken() {
        // Try to get from SecurityContext first (from UserPrincipal)
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
                UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
                String token = principal.token();
                if (token != null && !token.isEmpty()) {
                    log.debug("Got access token from SecurityContext (length: {})", token.length());
                    return token;
                }
            }
        } catch (Exception e) {
            log.debug("Could not get token from SecurityContext: {}", e.getMessage());
        }
        
        // Fallback to HttpServletRequest
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                log.warn("RequestContextHolder.getRequestAttributes() returned null - cannot get access token");
                return null;
            }
            
            HttpServletRequest request = attributes.getRequest();
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                log.warn("Authorization header not found or invalid format");
                return null;
            }
            
            String token = auth.substring(7);
            return token;
        } catch (Exception e) {
            log.error("Error getting access token from request: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get resident info by residentId - NOT cached to avoid caching null responses
     * We don't cache because:
     * 1. Token might expire
     * 2. We don't want to cache null responses from failed requests
     */
    public ResidentInfoResponse getResidentInfo(UUID residentId) {
        if (residentId == null) {
            return null;
        }

        // Get access token from current request
        String accessToken = getCurrentAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            log.warn("No access token available for fetching resident info: {}", residentId);
            return null;
        }
        
        return getResidentInfo(residentId, accessToken);
    }

    /**
     * Get resident info by residentId with access token
     */
    public ResidentInfoResponse getResidentInfo(UUID residentId, String accessToken) {
        if (residentId == null) {
            return null;
        }

        try {
            String url = baseServiceUrl + "/api/residents/" + residentId;
            
            var webClientBuilder = webClient
                    .get()
                    .uri(url);

            if (accessToken != null && !accessToken.isEmpty()) {
                webClientBuilder = webClientBuilder.header("Authorization", "Bearer " + accessToken);
            }

            Map<String, Object> response;
            try {
                response = webClientBuilder
                        .retrieve()
                        .onStatus(status -> status.value() == 404, clientResponse -> {
                            log.warn("Resident not found (404) for residentId: {}", residentId);
                            return clientResponse.bodyToMono(String.class)
                                    .flatMap(body -> {
                                        log.warn("404 Response body: {}", body);
                                        return Mono.error(WebClientResponseException.create(
                                                HttpStatus.NOT_FOUND.value(),
                                                "Resident not found: " + residentId,
                                                clientResponse.headers().asHttpHeaders(),
                                                body != null ? body.getBytes() : null,
                                                null
                                        ));
                                    });
                        })
                        .onStatus(status -> status.value() == 403, clientResponse -> {
                            log.error("403 Forbidden when fetching resident info for residentId: {}", residentId);
                            return clientResponse.bodyToMono(String.class)
                                    .flatMap(body -> {
                                        log.error("403 Response body: {}", body);
                                        return Mono.error(WebClientResponseException.create(
                                                HttpStatus.FORBIDDEN.value(),
                                                "Forbidden: " + residentId,
                                                clientResponse.headers().asHttpHeaders(),
                                                body != null ? body.getBytes() : null,
                                                null
                                        ));
                                    });
                        })
                        .bodyToMono(Map.class)
                        .block();
            } catch (WebClientResponseException.NotFound e) {
                log.warn("Resident not found (404) for residentId: {} - Response body: {}", residentId, e.getResponseBodyAsString());
                return null;
            } catch (WebClientResponseException.Forbidden e) {
                log.error("403 Forbidden when fetching resident info for residentId: {}. Response body: {}", residentId, e.getResponseBodyAsString());
                return null;
            } catch (WebClientResponseException e) {
                log.error("HTTP error when fetching resident info for residentId: {} - Status: {}, Body: {}", 
                        residentId, e.getStatusCode(), e.getResponseBodyAsString());
                return null;
            }

            if (response == null) {
                log.warn("Resident not found for residentId: {} - response is null", residentId);
                return null;
            }

            // Extract data from response - ResidentDto is a record, so fields are directly accessible
            String fullName = getStringValue(response, "fullName");
            String phone = getStringValue(response, "phone");
            String email = getStringValue(response, "email");
            
            // Get unit info by calling /api/residents/my-units endpoint
            // First, we need to get userId from the resident response
            UUID userId = null;
            Object userIdObj = response.get("userId");
            if (userIdObj != null) {
                try {
                    userId = UUID.fromString(userIdObj.toString());
                } catch (Exception e) {
                    log.warn("Failed to parse userId from response: {}", userIdObj);
                }
            }
            
            String unitNumber = null;
            UUID buildingId = null;
            String buildingName = null;
            
            // Get unit info for this specific resident (not the current logged-in user)
            // Use /api/residents/{residentId}/units endpoint
            if (accessToken != null && !accessToken.isEmpty()) {
                try {
                    String unitsUrl = baseServiceUrl + "/api/residents/" + residentId + "/units";
                    
                    List<Map<String, Object>> units = webClient
                            .get()
                            .uri(unitsUrl)
                            .header("Authorization", "Bearer " + accessToken)
                            .retrieve()
                            .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                            .block();
                    
                    if (units != null && !units.isEmpty()) {
                        Map<String, Object> firstUnit = units.get(0);
                        unitNumber = getStringValue(firstUnit, "code"); // UnitDto has "code" field
                        Object buildingIdObj = firstUnit.get("buildingId");
                        if (buildingIdObj != null) {
                            try {
                                buildingId = UUID.fromString(buildingIdObj.toString());
                            } catch (Exception e) {
                                log.warn("Failed to parse buildingId from units: {}", buildingIdObj);
                            }
                        }
                        
                        // Get building name from unit response (UnitDto already includes buildingName)
                        buildingName = getStringValue(firstUnit, "buildingName");
                        if (buildingName == null || buildingName.isEmpty()) {
                            // Fallback: Get building name from /api/buildings/{buildingId} if not in unit response
                            if (buildingId != null) {
                                try {
                                    String buildingUrl = baseServiceUrl + "/api/buildings/" + buildingId;
                                    
                                    Map<String, Object> building = webClient
                                            .get()
                                            .uri(buildingUrl)
                                            .header("Authorization", "Bearer " + accessToken)
                                            .retrieve()
                                            .bodyToMono(Map.class)
                                            .block();
                                    
                                    if (building != null) {
                                        buildingName = getStringValue(building, "name");
                                    }
                                } catch (Exception e) {
                                    log.warn("Failed to get building name for buildingId {}: {}", buildingId, e.getMessage());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to get unit info from /api/residents/{}/units: {}", residentId, e.getMessage());
                }
            }

            ResidentInfoResponse result = ResidentInfoResponse.builder()
                    .residentId(residentId)
                    .name(fullName)
                    .avatarUrl(null) // TODO: Add avatar URL if available
                    .unitNumber(unitNumber)
                    .buildingId(buildingId)
                    .buildingName(buildingName)
                    .build();
            
            return result;

        } catch (WebClientResponseException.Forbidden e) {
            log.error("403 Forbidden when fetching resident info for residentId: {}. Token may be invalid or expired.", residentId);
            return null;
        } catch (WebClientResponseException.NotFound e) {
            log.warn("Resident not found for residentId: {}", residentId);
            return null;
        } catch (WebClientResponseException e) {
            log.error("HTTP error when fetching resident info for residentId: {} - Status: {}, Body: {}", 
                    residentId, e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.error("Error fetching resident info for residentId: {}", residentId, e);
            return null;
        }
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        return value.toString();
    }
}

