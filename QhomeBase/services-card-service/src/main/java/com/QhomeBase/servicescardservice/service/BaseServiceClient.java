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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class BaseServiceClient {

    private final RestTemplate restTemplate;

    @Value("${base.service.base-url:http://localhost:8081/api}")
    private String baseServiceUrl;

    /**
     * Kiểm tra xem cư dân có AccountCreationRequest với status = APPROVED không
     * Logic: Nếu resident đã có userId (đã có account) thì có nghĩa là đã được approve.
     * Nếu chưa có userId, kiểm tra xem có AccountCreationRequest với status = APPROVED không.
     * @param residentId ID của cư dân
     * @param accessToken Access token để authenticate với base-service
     * @return true nếu đã được approve thành thành viên, false nếu chưa được approve
     */
    public boolean isResidentMemberApproved(UUID residentId, String accessToken) {
        if (residentId == null) {
            log.warn("⚠️ [BaseServiceClient] residentId is null");
            return false;
        }

        try {
            // Sử dụng endpoint /api/residents/{residentId} thay vì /account để tránh vấn đề authentication
            // Endpoint này không yêu cầu role RESIDENT và có thể được gọi từ service-to-service
            String url = baseServiceUrl + "/residents/" + residentId;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (accessToken != null && !accessToken.isEmpty()) {
                headers.setBearerAuth(accessToken);
            }
            
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            log.info("🔍 [BaseServiceClient] Checking account approval for residentId: {} | URL: {}", residentId, url);
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    Map.class
            );
            
            // Kiểm tra xem resident có userId không (có userId = đã có account = đã được approve)
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> resident = response.getBody();
                Object userIdObj = resident.get("userId");
                
                if (userIdObj != null && !userIdObj.toString().isEmpty() && !"null".equalsIgnoreCase(userIdObj.toString())) {
                    log.info("✅ [BaseServiceClient] Resident {} đã có userId (account), đã được approve | userId: {}", 
                            residentId, userIdObj);
                return true;
                } else {
                    log.warn("⚠️ [BaseServiceClient] Resident {} chưa có userId (chưa có account), chưa được approve", residentId);
                    return false;
                }
            } else if (response.getStatusCode().value() == 404) {
                // Resident không tồn tại
                log.warn("⚠️ [BaseServiceClient] Resident {} không tồn tại (404)", residentId);
                return false;
            } else {
                log.warn("⚠️ [BaseServiceClient] Unexpected response status: {} for residentId: {}", 
                        response.getStatusCode(), residentId);
                return false;
            }
        } catch (RestClientException e) {
            log.error("❌ [BaseServiceClient] ========== ERROR CHECKING ACCOUNT APPROVAL ==========");
            log.error("❌ [BaseServiceClient] Error checking account approval for residentId {}: {}", 
                    residentId, e.getMessage());
            log.error("❌ [BaseServiceClient] Exception type: {}", e.getClass().getName());
            if (e.getCause() != null) {
                log.error("❌ [BaseServiceClient] Caused by: {}", e.getCause().getMessage());
            }
            // Nếu không thể kiểm tra được (service down, network error), 
            // thì để an toàn, không cho phép đăng ký
            return false;
        }
    }

    /**
     * Kiểm tra xem user có phải là OWNER (chủ căn hộ) của unit không
     * OWNER được định nghĩa là:
     * - household.kind == OWNER HOẶC TENANT (người mua hoặc người thuê căn hộ)
     * - VÀ user là primaryResidentId của household đó
     * @param userId ID của user
     * @param unitId ID của căn hộ
     * @param accessToken Access token để authenticate với base-service
     * @return true nếu user là OWNER của unit, false nếu không
     */
    public boolean isOwnerOfUnit(UUID userId, UUID unitId, String accessToken) {
        if (userId == null || unitId == null) {
            log.warn("⚠️ [BaseServiceClient] userId or unitId is null");
            return false;
        }

        try {
            // Lấy household info từ base-service
            String url = baseServiceUrl + "/households/units/" + unitId + "/current";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (accessToken != null && !accessToken.isEmpty()) {
                headers.setBearerAuth(accessToken);
            }
            
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            log.debug("🔍 [BaseServiceClient] Checking if user {} is OWNER of unit {}", userId, unitId);
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> household = response.getBody();
                
                // Kiểm tra household kind - OWNER hoặc TENANT đều được coi là chủ căn hộ
                Object kindObj = household.get("kind");
                if (kindObj == null) {
                    log.debug("⚠️ [BaseServiceClient] Household kind is null");
                    return false;
                }
                String kind = kindObj.toString();
                if (!"OWNER".equalsIgnoreCase(kind) && !"TENANT".equalsIgnoreCase(kind)) {
                    log.debug("⚠️ [BaseServiceClient] Household kind is not OWNER or TENANT: {}", kind);
                    return false;
                }
                
                // Kiểm tra primaryResidentId
                Object primaryResidentIdObj = household.get("primaryResidentId");
                if (primaryResidentIdObj == null) {
                    log.debug("⚠️ [BaseServiceClient] Household has no primaryResidentId");
                    return false;
                }
                
                // Lấy residentId từ userId
                String residentUrl = baseServiceUrl + "/residents/by-user/" + userId;
                ResponseEntity<Map> residentResponse = restTemplate.exchange(
                        residentUrl,
                        HttpMethod.GET,
                        request,
                        Map.class
                );
                
                if (residentResponse.getStatusCode().is2xxSuccessful() && residentResponse.getBody() != null) {
                    Map<String, Object> resident = residentResponse.getBody();
                    Object residentIdObj = resident.get("id");
                    
                    if (residentIdObj != null) {
                        String residentId = residentIdObj.toString();
                        String primaryResidentId = primaryResidentIdObj.toString();
                        
                        boolean isOwner = residentId.equals(primaryResidentId);
                        log.debug("✅ [BaseServiceClient] User {} isOwner of unit {}: {}", userId, unitId, isOwner);
                        return isOwner;
                    }
                }
            }
            
            return false;
        } catch (RestClientException e) {
            log.error("❌ [BaseServiceClient] Error checking if user {} is OWNER of unit {}: {}", 
                    userId, unitId, e.getMessage());
            return false;
        }
    }

    /**
     * Tìm residentId từ userId
     * @param userId ID của user
     * @param accessToken Access token để authenticate với base-service
     * @return UUID của resident nếu tìm thấy, null nếu không tìm thấy
     */
    public UUID findResidentIdByUserId(UUID userId, String accessToken) {
        if (userId == null) {
            log.warn("⚠️ [BaseServiceClient] userId is null");
            return null;
        }

        try {
            String residentUrl = baseServiceUrl + "/residents/by-user/" + userId;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (accessToken != null && !accessToken.isEmpty()) {
                headers.setBearerAuth(accessToken);
            }
            
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            log.debug("🔍 [BaseServiceClient] Finding residentId for userId: {}", userId);
            ResponseEntity<Map> response = restTemplate.exchange(
                    residentUrl,
                    HttpMethod.GET,
                    request,
                    Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> resident = response.getBody();
                Object residentIdObj = resident.get("id");
                
                if (residentIdObj != null) {
                    UUID residentId = UUID.fromString(residentIdObj.toString());
                    log.debug("✅ [BaseServiceClient] Found residentId {} for userId {}", residentId, userId);
                    return residentId;
                }
            }
            
            log.warn("⚠️ [BaseServiceClient] No resident found for userId: {}", userId);
            return null;
        } catch (RestClientException e) {
            log.error("❌ [BaseServiceClient] Error finding residentId for userId {}: {}", 
                    userId, e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("❌ [BaseServiceClient] Unexpected error finding residentId for userId {}: {}", 
                    userId, e.getMessage());
            return null;
        }
    }
}
