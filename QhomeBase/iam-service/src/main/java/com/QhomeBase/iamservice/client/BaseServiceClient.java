package com.QhomeBase.iamservice.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class BaseServiceClient {

    private final WebClient baseServiceWebClient;

    public void syncStaffResident(UUID userId, String fullName, String email, String phone) {
        if (userId == null) {
            log.warn("Cannot sync staff resident: userId is null");
            return;
        }
        StaffResidentSyncRequest request = new StaffResidentSyncRequest(
                userId,
                fullName,
                email,
                phone);
        try {
            baseServiceWebClient
                    .post()
                    .uri("/api/residents/staff/sync")
                    .headers(headers -> {
                        String token = extractToken();
                        if (token != null) {
                            headers.setBearerAuth(token);
                        }
                    })
                    .bodyValue(request)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception e) {
            log.warn("Failed to sync staff resident for user {}: {}", userId, e.getMessage());
        }
    }

    public List<UUID> getResidentUserIdsByBuilding(UUID buildingId, Integer floor) {
        if (buildingId == null) {
            return List.of();
        }
        try {
            return baseServiceWebClient
                    .get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/api/residents/user-ids-by-building/{buildingId}");
                        if (floor != null) {
                            uriBuilder.queryParam("floor", floor);
                        }
                        return uriBuilder.build(buildingId);
                    })
                    .headers(headers -> {
                        String token = extractToken();
                        if (token != null) {
                            headers.setBearerAuth(token);
                        }
                    })
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<UUID>>() {
                    })
                    .block();
        } catch (Exception e) {
            log.error("Error fetching resident user IDs by building {}, floor {}: {}", buildingId, floor,
                    e.getMessage());
            return List.of();
        }
    }

    private String extractToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.getPrincipal() instanceof com.QhomeBase.iamservice.security.UserPrincipal principal) {
            return principal.token();
        }
        return null;
    }

    private record StaffResidentSyncRequest(
            UUID userId,
            String fullName,
            String email,
            String phone) {
    }
}
