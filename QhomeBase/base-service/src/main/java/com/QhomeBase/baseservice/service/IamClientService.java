package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.ResidentAccountDto;
import com.QhomeBase.baseservice.security.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class IamClientService {
    
    private final WebClient webClient;

    public IamClientService(@Qualifier("iamWebClient") WebClient webClient) {
        this.webClient = webClient;
    }
    
    public ResidentAccountDto createUserForResident(
            String username, 
            String email, 
            String password, 
            boolean autoGenerate,
            UUID residentId) {
        return createUserForResident(username, email, password, autoGenerate, residentId, null, null);
    }
    
    public ResidentAccountDto createUserForResident(
            String username, 
            String email, 
            String password, 
            boolean autoGenerate,
            UUID residentId,
            String token) {
        return createUserForResident(username, email, password, autoGenerate, residentId, token, null);
    }
    
    public ResidentAccountDto createUserForResident(
            String username, 
            String email, 
            String password, 
            boolean autoGenerate,
            UUID residentId,
            String token,
            String buildingName) {
        
        CreateUserRequest request = new CreateUserRequest(
                username,
                email,
                password,
                autoGenerate,
                residentId,
                buildingName
        );
        
        try {
            String authToken = token != null ? token : getCurrentToken();
            
            UserAccountResponse response;
            if (authToken != null && !authToken.isEmpty()) {
                response = webClient
                        .post()
                        .uri("/api/users/create-for-resident")
                        .header("Authorization", "Bearer " + authToken)
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(UserAccountResponse.class)
                        .timeout(java.time.Duration.ofSeconds(30)) // 30 seconds timeout
                        .doOnError(error -> log.error("Error in WebClient call to IAM service: {}", error.getMessage(), error))
                        .block();
            } else {
                log.error("No JWT token available when calling IAM service - this will cause 403 Forbidden");
                response = webClient
                        .post()
                        .uri("/api/users/create-for-resident")
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(UserAccountResponse.class)
//                        .timeout(java.time.Duration.ofSeconds(30)) // 30 seconds timeout
                        .doOnError(error -> log.error("Error in WebClient call to IAM service: {}", error.getMessage(), error))
                        .block();
            }
            
            if (response == null) {
                log.error("IAM service returned null response for create user request. Request: username={}, email={}, autoGenerate={}, residentId={}", 
                        request.username(), request.email(), request.autoGenerate(), request.residentId());
                throw new RuntimeException("IAM service returned null response. This may indicate the service is unavailable or returned an empty response.");
            }
            
            return new ResidentAccountDto(
                    response.userId(),
                    response.username(),
                    response.email(),
                    response.roles(),
                    response.active()
            );
        } catch (WebClientResponseException e) {
            String errorMessage = e.getResponseBodyAsString();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "No error details provided by IAM service (status: " + e.getStatusCode() + ")";
            }
            log.error("Error calling IAM service to create user: status={}, message={}, body={}, request={}", 
                    e.getStatusCode(), e.getMessage(), errorMessage, 
                    "username=" + request.username() + ", email=" + request.email() + ", autoGenerate=" + request.autoGenerate());
            
            // Try to parse error response if it's JSON
            try {
                if (errorMessage.contains("\"message\"")) {
                    // Extract message from JSON response
                    int messageStart = errorMessage.indexOf("\"message\"");
                    if (messageStart > 0) {
                        int colonIndex = errorMessage.indexOf(":", messageStart);
                        int quoteStart = errorMessage.indexOf("\"", colonIndex) + 1;
                        int quoteEnd = errorMessage.indexOf("\"", quoteStart);
                        if (quoteEnd > quoteStart) {
                            errorMessage = errorMessage.substring(quoteStart, quoteEnd);
                        }
                    }
                }
            } catch (Exception parseEx) {
                // Ignore parsing errors, use original error message
            }
            
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                throw new IllegalArgumentException("Failed to create user account: " + errorMessage);
            }
            if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
                throw new IllegalStateException("Access denied when creating user account. Check authentication token.");
            }
            throw new RuntimeException("Failed to create user account: " + errorMessage, e);
        } catch (org.springframework.web.reactive.function.client.WebClientRequestException e) {
            // Connection/timeout errors (includes timeout exceptions from Reactor)
            String errorMessage;
            if (e.getMessage() != null && e.getMessage().contains("timeout")) {
                errorMessage = "IAM service request timed out. The service may be overloaded or unavailable.";
            } else {
                errorMessage = "Cannot connect to IAM service. Please check if IAM service is running and accessible.";
            }
            log.error("Connection/timeout error calling IAM service: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create user account: " + errorMessage, e);
        } catch (Exception e) {
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Unknown error";
            String errorType = e.getClass().getSimpleName();
            log.error("Unexpected error calling IAM service (type: {}): {}", errorType, errorMessage, e);
            throw new RuntimeException("Failed to create user account: " + errorMessage, e);
        }
    }
    
    public ResidentAccountDto getUserAccountInfo(UUID userId) {
        try {
            String token = getCurrentToken();
            
            var webClientBuilder = webClient
                    .get()
                    .uri("/api/users/{userId}/account-info", userId);
            
            if (token != null && !token.isEmpty()) {
                webClientBuilder = webClientBuilder.header("Authorization", "Bearer " + token);
            } else {
                log.warn("No token available when calling IAM service for userId: {}", userId);
            }
            
            UserAccountResponse response = webClientBuilder
                    .retrieve()
                    .bodyToMono(UserAccountResponse.class)
                    .timeout(java.time.Duration.ofSeconds(5)) // 5 seconds timeout
                    .block();
            
            if (response == null) {
                log.warn("IAM service returned null response for userId: {}", userId);
                return null;
            }
            
            return new ResidentAccountDto(
                    response.userId(),
                    response.username(),
                    response.email(),
                    response.roles(),
                    response.active()
            );
        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return null;
            }
            // Only log stacktrace for >=500 errors (production-ready)
            if (e.getStatusCode().value() >= 500) {
                log.error("Error calling IAM service to get user account for userId {}: status={}, message={}", 
                        userId, e.getStatusCode(), e.getMessage(), e);
                // For 500 errors, return null instead of throwing to allow graceful degradation
                return null;
            } else {
                log.warn("Error calling IAM service to get user account for userId {}: status={}, message={}", 
                        userId, e.getStatusCode(), e.getMessage());
                throw new RuntimeException("Failed to get user account: " + e.getMessage() + " (status: " + e.getStatusCode() + ")", e);
            }
        } catch (Exception e) {
            // Only log stacktrace for unexpected errors (production-ready)
            log.error("Unexpected error calling IAM service for userId {}: {}", userId, e.getMessage(), e);
            // Return null instead of throwing to allow graceful degradation
            return null;
        }
    }
    
    public boolean usernameExists(String username) {
        return usernameExists(username, null);
    }
    
    public boolean usernameExists(String username, String token) {
        try {
            String authToken = token != null ? token : getCurrentToken();
            var webClientBuilder = webClient.get()
                    .uri("/api/users/by-username/{username}", username);
            
            if (authToken != null && !authToken.isEmpty()) {
                webClientBuilder = webClientBuilder.header("Authorization", "Bearer " + authToken);
            }
            
            webClientBuilder
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
            return true;
        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return false;
            }
            log.error("Error checking username existence: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Unexpected error checking username existence", e);
            return false;
        }
    }
    
    public boolean emailExists(String email) {
        return emailExists(email, null);
    }
    
    public boolean emailExists(String email, String token) {
        try {
            String authToken = token != null ? token : getCurrentToken();
            var webClientBuilder = webClient.get()
                    .uri("/api/users/by-email/{email}", email);
            
            if (authToken != null && !authToken.isEmpty()) {
                webClientBuilder = webClientBuilder.header("Authorization", "Bearer " + authToken);
            }
            
            webClientBuilder
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
            return true;
        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return false;
            }
            log.error("Error checking email existence: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Unexpected error checking email existence", e);
            return false;
        }
    }
    
    private record CreateUserRequest(
            String username,
            String email,
            String password,
            boolean autoGenerate,
            UUID residentId,
            String buildingName
    ) {}
    
    private record UserAccountResponse(
            UUID userId,
            String username,
            String email,
            List<String> roles,
            boolean active
    ) {}
    
    private String getCurrentToken() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
                String token = principal.token();
                if (token != null && !token.isEmpty()) {
                    return token;
                }
            } else {
                log.warn("No UserPrincipal found in SecurityContext. Authentication: {}", auth);
            }
        } catch (Exception e) {
            log.error("Failed to get current token: {}", e.getMessage(), e);
        }
        return null;
    }
}

