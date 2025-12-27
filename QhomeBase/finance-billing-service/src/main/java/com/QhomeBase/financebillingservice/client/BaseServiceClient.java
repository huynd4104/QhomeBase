package com.QhomeBase.financebillingservice.client;

import com.QhomeBase.financebillingservice.dto.ReadingCycleDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class BaseServiceClient {

    private final WebClient webClient;

    public List<ReadingCycleDto> getAllReadingCycles() {
        try {
            return webClient.get()
                    .uri("/api/reading-cycles")
                    .retrieve()
                    .onStatus(status -> status.isError(), response -> {
                        log.error("Error response from base-service when fetching reading cycles: {}", response.statusCode());
                        return response.createException().map(ex -> {
                            log.error("Exception details: {}", ex.getMessage());
                            return ex;
                        });
                    })
                    .bodyToFlux(ReadingCycleDto.class)
                    .collectList()
                    .block();
        } catch (Exception e) {
            log.error("Error fetching reading cycles from base-service: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch reading cycles from base-service: " + e.getMessage(), e);
        }
    }

    public ReadingCycleDto getReadingCycleById(UUID cycleId) {
        try {
            return webClient.get()
                    .uri("/api/reading-cycles/{cycleId}", cycleId)
                    .retrieve()
                    .onStatus(status -> status.isError(), response -> {
                        log.error("Error response from base-service when fetching reading cycle {}: {}", cycleId, response.statusCode());
                        return response.createException().map(ex -> {
                            log.error("Exception details: {}", ex.getMessage());
                            return ex;
                        });
                    })
                    .bodyToMono(ReadingCycleDto.class)
                    .block();
        } catch (Exception e) {
            log.error("Error fetching reading cycle {} from base-service: {}", cycleId, e.getMessage());
            throw new RuntimeException("Failed to fetch reading cycle from base-service: " + e.getMessage(), e);
        }
    }

    public UnitInfo getUnitById(UUID unitId) {
        try {
            return webClient.get()
                    .uri("/api/units/{unitId}", unitId)
                    .retrieve()
                    .bodyToMono(UnitInfo.class)
                    .block();
        } catch (Exception e) {
            log.warn("Error fetching unit {} from base-service: {}", unitId, e.getMessage());
            return null;
        }
    }

    public List<ServiceInfo> getAllServices() {
        try {
            return webClient.get()
                    .uri("/api/services")
                    .retrieve()
                    .onStatus(status -> status.isError(), response -> {
                        log.warn("Error response from base-service when fetching services: {}", response.statusCode());
                        return response.createException().map(ex -> {
                            log.warn("Exception details: {}", ex.getMessage());
                            return ex;
                        });
                    })
                    .bodyToFlux(ServiceInfo.class)
                    .collectList()
                    .block();
        } catch (Exception e) {
            log.warn("Error fetching services from base-service: {}", e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    public ServiceInfo.HouseholdInfo getCurrentHouseholdByUnitId(UUID unitId) {
        try {
            Map<String, Object> householdMap = (Map<String, Object>) (Object) webClient.get()
                    .uri("/api/households/units/{unitId}/current", unitId)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            
            if (householdMap == null) {
                return null;
            }
            
            ServiceInfo.HouseholdInfo household = new ServiceInfo.HouseholdInfo();
            
            Object idObj = householdMap.get("id");
            if (idObj != null) {
                UUID id = idObj instanceof UUID ? (UUID) idObj : UUID.fromString(idObj.toString());
                household.setId(id);
            }
            
            Object unitIdObj = householdMap.get("unitId");
            if (unitIdObj != null) {
                UUID unitIdUuid = unitIdObj instanceof UUID ? (UUID) unitIdObj : UUID.fromString(unitIdObj.toString());
                household.setUnitId(unitIdUuid);
            }
            
            Object primaryResidentIdObj = householdMap.get("primaryResidentId");
            if (primaryResidentIdObj != null) {
                UUID primaryResidentId = primaryResidentIdObj instanceof UUID 
                        ? (UUID) primaryResidentIdObj 
                        : UUID.fromString(primaryResidentIdObj.toString());
                household.setPrimaryResidentId(primaryResidentId);
            }
            
            Object kindObj = householdMap.get("kind");
            if (kindObj != null) {
                household.setKind(kindObj.toString());
            }
            
            return household;
        } catch (Exception e) {
            log.warn("Error fetching current household for unit {} from base-service: {}", unitId, e.getMessage());
            return null;
        }
    }

    public List<ServiceInfo.HouseholdMemberInfo> getActiveMembersByHouseholdId(UUID householdId) {
        try {
            // Use pagination to avoid DataBufferLimitException
            // Fetch in batches of 100 members at a time
            List<ServiceInfo.HouseholdMemberInfo> allMembers = new java.util.ArrayList<>();
            final int limit = 100;
            int currentOffset = 0;
            boolean hasMore = true;
            
            while (hasMore) {
                final int offset = currentOffset; // Make effectively final for lambda
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> response = (java.util.Map<String, Object>) (Object) webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/api/household-members/households/{householdId}")
                                .queryParam("limit", limit)
                                .queryParam("offset", offset)
                                .build(householdId))
                        .retrieve()
                        .bodyToMono(java.util.Map.class)
                        .block();
                
                if (response == null) {
                    break;
                }
                
                @SuppressWarnings("unchecked")
                List<java.util.Map<String, Object>> membersList = 
                        (List<java.util.Map<String, Object>>) response.get("members");
                
                if (membersList != null) {
                    for (java.util.Map<String, Object> memberMap : membersList) {
                        ServiceInfo.HouseholdMemberInfo member = new ServiceInfo.HouseholdMemberInfo();
                        
                        Object idObj = memberMap.get("id");
                        if (idObj != null) {
                            UUID id = idObj instanceof UUID ? (UUID) idObj : UUID.fromString(idObj.toString());
                            member.setId(id);
                        }
                        
                        Object householdIdObj = memberMap.get("householdId");
                        if (householdIdObj != null) {
                            UUID hId = householdIdObj instanceof UUID ? (UUID) householdIdObj : UUID.fromString(householdIdObj.toString());
                            member.setHouseholdId(hId);
                        }
                        
                        Object residentIdObj = memberMap.get("residentId");
                        if (residentIdObj != null) {
                            UUID rId = residentIdObj instanceof UUID ? (UUID) residentIdObj : UUID.fromString(residentIdObj.toString());
                            member.setResidentId(rId);
                        }
                        
                        Object residentNameObj = memberMap.get("residentName");
                        if (residentNameObj != null) {
                            member.setResidentName(residentNameObj.toString());
                        }
                        
                        Object isPrimaryObj = memberMap.get("isPrimary");
                        if (isPrimaryObj != null) {
                            member.setIsPrimary(Boolean.TRUE.equals(isPrimaryObj) || 
                                              (isPrimaryObj instanceof Boolean && (Boolean) isPrimaryObj));
                        }
                        
                        allMembers.add(member);
                    }
                }
                
                // Check if there are more members to fetch
                Object totalObj = response.get("total");
                long total = totalObj instanceof Number ? ((Number) totalObj).longValue() : 0;
                currentOffset += limit;
                hasMore = currentOffset < total;
            }
            
            return allMembers;
        } catch (Exception e) {
            log.warn("Error fetching active members for household {} from base-service: {}", householdId, e.getMessage());
            return List.of();
        }
    }

    /**
     * Lấy unitId từ residentId thông qua household member
     * Trả về unitId của household mà resident này là member (active)
     */
    @SuppressWarnings("unchecked")
    public UUID getUnitIdFromResidentId(UUID residentId) {
        try {
            // Lấy household members của resident này
            // Endpoint trả về List<HouseholdMemberDto> với householdId
            List<Map<String, Object>> members = (List<Map<String, Object>>) (Object) webClient.get()
                    .uri("/api/household-members/residents/{residentId}", residentId)
                    .retrieve()
                    .bodyToFlux(Map.class)
                    .collectList()
                    .block();
            
            if (members == null || members.isEmpty()) {
                log.warn("No household members found for resident {}", residentId);
                return null;
            }
            
            // Lấy householdId từ member đầu tiên (thường resident chỉ thuộc 1 household tại 1 thời điểm)
            Map<String, Object> member = members.get(0);
            if (member == null || !member.containsKey("householdId")) {
                return null;
            }
            
            Object householdIdObj = member.get("householdId");
            if (householdIdObj == null) {
                return null;
            }
            
            UUID householdId;
            if (householdIdObj instanceof UUID) {
                householdId = (UUID) householdIdObj;
            } else if (householdIdObj instanceof String) {
                householdId = UUID.fromString((String) householdIdObj);
            } else {
                return null;
            }
            
            // Lấy household info để lấy unitId
            ServiceInfo.HouseholdInfo household = getHouseholdById(householdId);
            if (household != null && household.getUnitId() != null) {
                return household.getUnitId();
            }
            
            return null;
        } catch (Exception e) {
            log.warn("Error fetching unitId from residentId {} from base-service: {}", residentId, e.getMessage());
            return null;
        }
    }

    /**
     * Lấy household info theo householdId
     */
    @SuppressWarnings("unchecked")
    private ServiceInfo.HouseholdInfo getHouseholdById(UUID householdId) {
        try {
            Map<String, Object> householdMap = (Map<String, Object>) (Object) webClient.get()
                    .uri("/api/households/{householdId}", householdId)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            
            if (householdMap == null) {
                return null;
            }
            
            ServiceInfo.HouseholdInfo household = new ServiceInfo.HouseholdInfo();
            
            Object idObj = householdMap.get("id");
            if (idObj != null) {
                UUID id = idObj instanceof UUID ? (UUID) idObj : UUID.fromString(idObj.toString());
                household.setId(id);
            }
            
            Object unitIdObj = householdMap.get("unitId");
            if (unitIdObj != null) {
                UUID unitId = unitIdObj instanceof UUID ? (UUID) unitIdObj : UUID.fromString(unitIdObj.toString());
                household.setUnitId(unitId);
            }
            
            Object primaryResidentIdObj = householdMap.get("primaryResidentId");
            if (primaryResidentIdObj != null) {
                UUID primaryResidentId = primaryResidentIdObj instanceof UUID 
                        ? (UUID) primaryResidentIdObj 
                        : UUID.fromString(primaryResidentIdObj.toString());
                household.setPrimaryResidentId(primaryResidentId);
            }
            
            Object kindObj = householdMap.get("kind");
            if (kindObj != null) {
                household.setKind(kindObj.toString());
            }
            
            return household;
        } catch (Exception e) {
            log.warn("Error fetching household {} from base-service: {}", householdId, e.getMessage());
            return null;
        }
    }

    /**
     * Kiểm tra xem user có phải là OWNER (chủ căn hộ) của unit không
     * OWNER được định nghĩa là:
     * - household.kind == OWNER HOẶC TENANT (người mua hoặc người thuê căn hộ)
     * - VÀ user là primaryResidentId của household đó
     * @param userId ID của user
     * @param unitId ID của căn hộ
     * @return true nếu user là OWNER của unit, false nếu không
     */
    public boolean isOwnerOfUnit(UUID userId, UUID unitId) {
        if (userId == null || unitId == null) {
            log.warn("⚠️ [BaseServiceClient] userId or unitId is null");
            return false;
        }

        try {
            // Lấy household info từ base-service
            ServiceInfo.HouseholdInfo household = getCurrentHouseholdByUnitId(unitId);
            if (household == null || household.getId() == null) {
                log.debug("⚠️ [BaseServiceClient] No household found for unit {}", unitId);
                return false;
            }
            
            // Kiểm tra household kind - OWNER hoặc TENANT đều được coi là chủ căn hộ
            if (!household.isOwner() && !household.isTenant()) {
                log.debug("⚠️ [BaseServiceClient] Household kind is not OWNER or TENANT: {}", household.getKind());
                return false;
            }
            
            // Kiểm tra primaryResidentId
            if (household.getPrimaryResidentId() == null) {
                log.debug("⚠️ [BaseServiceClient] Household has no primaryResidentId");
                return false;
            }
            
            // Lấy residentId từ userId
            @SuppressWarnings("unchecked")
            Map<String, Object> residentMap = (Map<String, Object>) (Object) webClient.get()
                    .uri("/api/residents/by-user/{userId}", userId)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            
            if (residentMap == null) {
                log.debug("⚠️ [BaseServiceClient] No resident found for userId {}", userId);
                return false;
            }
            
            Object residentIdObj = residentMap.get("id");
            if (residentIdObj == null) {
                return false;
            }
            
            UUID residentId;
            if (residentIdObj instanceof UUID) {
                residentId = (UUID) residentIdObj;
            } else {
                residentId = UUID.fromString(residentIdObj.toString());
            }
            
            boolean isOwner = residentId.equals(household.getPrimaryResidentId());
            log.debug("✅ [BaseServiceClient] User {} isOwner of unit {}: {}", userId, unitId, isOwner);
            return isOwner;
        } catch (Exception e) {
            log.error("❌ [BaseServiceClient] Error checking if user {} is OWNER of unit {}: {}", 
                    userId, unitId, e.getMessage());
            return false;
        }
    }

    public static class UnitInfo {
        private UUID id;
        private UUID buildingId;
        private String code;
        private String name;
        private Integer floor;

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public UUID getBuildingId() {
            return buildingId;
        }

        public void setBuildingId(UUID buildingId) {
            this.buildingId = buildingId;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getFloor() {
            return floor;
        }

        public void setFloor(Integer floor) {
            this.floor = floor;
        }
    }

    public static class ServiceInfo {
        private UUID id;
        private String code;
        private String name;

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }


        public static class HouseholdInfo {
            private UUID id;
            private UUID unitId;
            private UUID primaryResidentId;
            private String kind; // OWNER, TENANT, SERVICE

            public UUID getId() {
                return id;
            }

            public void setId(UUID id) {
                this.id = id;
            }

            public UUID getUnitId() {
                return unitId;
            }

            public void setUnitId(UUID unitId) {
                this.unitId = unitId;
            }

            public UUID getPrimaryResidentId() {
                return primaryResidentId;
            }

            public void setPrimaryResidentId(UUID primaryResidentId) {
                this.primaryResidentId = primaryResidentId;
            }

            public String getKind() {
                return kind;
            }

            public void setKind(String kind) {
                this.kind = kind;
            }
            
            public boolean isOwner() {
                return "OWNER".equalsIgnoreCase(kind);
            }
            
            public boolean isTenant() {
                return "TENANT".equalsIgnoreCase(kind);
            }
        }

        public static class HouseholdMemberInfo {
            private UUID id;
            private UUID householdId;
            private UUID residentId;
            private String residentName;
            private Boolean isPrimary;

            public UUID getId() {
                return id;
            }

            public void setId(UUID id) {
                this.id = id;
            }

            public UUID getHouseholdId() {
                return householdId;
            }

            public void setHouseholdId(UUID householdId) {
                this.householdId = householdId;
            }

            public UUID getResidentId() {
                return residentId;
            }

            public void setResidentId(UUID residentId) {
                this.residentId = residentId;
            }

            public String getResidentName() {
                return residentName;
            }

            public void setResidentName(String residentName) {
                this.residentName = residentName;
            }

            public Boolean getIsPrimary() {
                return isPrimary;
            }

            public void setIsPrimary(Boolean isPrimary) {
                this.isPrimary = isPrimary;
            }
        }
    }
}
