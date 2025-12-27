package com.QhomeBase.customerinteractionservice.client;

import com.QhomeBase.customerinteractionservice.client.dto.IamUserInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class IamServiceClient {

    private final WebClient iamWebClient;

    public IamUserInfoResponse fetchUserInfo(UUID userId) {
        if (userId == null) {
            return null;
        }
        try {
            return iamWebClient
                    .get()
                    .uri("/api/users/{userId}", userId)
                    .retrieve()
                    .bodyToMono(IamUserInfoResponse.class)
                    .block();
        } catch (Exception e) {
            log.warn("Failed to fetch IAM user info for {}: {}", userId, e.getMessage());
            return null;
        }
    }
}





