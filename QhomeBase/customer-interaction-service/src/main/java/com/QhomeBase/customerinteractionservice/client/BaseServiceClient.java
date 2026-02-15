package com.QhomeBase.customerinteractionservice.client;

import com.QhomeBase.customerinteractionservice.client.dto.HouseholdDto;
import com.QhomeBase.customerinteractionservice.client.dto.HouseholdMemberDto;
import com.QhomeBase.customerinteractionservice.client.dto.ResidentResponse;
import com.QhomeBase.customerinteractionservice.client.dto.UnitDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class BaseServiceClient {

    @Qualifier("baseServiceWebClient")
    private final WebClient baseServiceWebClient;

    public BaseServiceClient(WebClient baseServiceWebClient) {this.baseServiceWebClient = baseServiceWebClient;}

    public ResidentResponse getResidentById(UUID residentId) {
        if (residentId == null) {
            return null;
        }
        try {
            return baseServiceWebClient
                    .get()
                    .uri("/api/residents/{residentId}", residentId)
                    .retrieve()
                    .bodyToMono(ResidentResponse.class)
                    .block();
        } catch (WebClientResponseException.NotFound notFound) {
            log.debug("Resident not found for id {}", residentId);
            return null;
        } catch (Exception e) {
            log.warn("Failed to fetch resident by id {}: {}", residentId, e.getMessage());
            return null;
        }
    }

    public ResidentResponse getResidentByUserId(UUID userId) {
        if (userId == null) {
            return null;
        }
        try {
            return baseServiceWebClient
                    .get()
                    .uri("/api/residents/by-user/{userId}", userId)
                    .retrieve()
                    .bodyToMono(ResidentResponse.class)
                    .block();
        } catch (WebClientResponseException.NotFound notFound) {
            log.debug("Resident not found for user {}", userId);
            return null;
        } catch (Exception e) {
            log.warn("Failed to fetch resident by user {}: {}", userId, e.getMessage());
            return null;
        }
    }

    public UUID getResidentIdByUserId(UUID userId) {
        ResidentResponse resident = getResidentByUserId(userId);
        return resident != null ? resident.id() : null;
    }

    public String fetchResidentNameById(UUID residentId) {
        ResidentResponse resident = getResidentById(residentId);
        return resident != null ? resident.fullName() : null;
    }

    public List<HouseholdMemberDto> getActiveHouseholdMembersByResident(UUID residentId) {
        if (residentId == null) {
            return Collections.emptyList();
        }
        try {
            return baseServiceWebClient
                    .get()
                    .uri("/api/household-members/residents/{residentId}", residentId)
                    .retrieve()
                    .bodyToFlux(HouseholdMemberDto.class)
                    .collectList()
                    .block();
        } catch (WebClientResponseException.NotFound notFound) {
            log.debug("No household members found for resident {}", residentId);
            return Collections.emptyList();
        } catch (Exception e) {
            log.warn("Failed to fetch household members for resident {}: {}", residentId, e.getMessage());
            return Collections.emptyList();
        }
    }

    public HouseholdDto getHouseholdById(UUID householdId) {
        if (householdId == null) {
            return null;
        }
        try {
            return baseServiceWebClient
                    .get()
                    .uri("/api/households/{householdId}", householdId)
                    .retrieve()
                    .bodyToMono(HouseholdDto.class)
                    .block();
        } catch (WebClientResponseException.NotFound notFound) {
            log.debug("Household not found for id {}", householdId);
            return null;
        } catch (Exception e) {
            log.warn("Failed to fetch household {}: {}", householdId, e.getMessage());
            return null;
        }
    }

    public UnitDto getUnitById(UUID unitId) {
        if (unitId == null) {
            return null;
        }
        try {
            return baseServiceWebClient
                    .get()
                    .uri("/api/units/{unitId}", unitId)
                    .retrieve()
                    .bodyToMono(UnitDto.class)
                    .block();
        } catch (WebClientResponseException.NotFound notFound) {
            log.debug("Unit not found for id {}", unitId);
            return null;
        } catch (Exception e) {
            log.warn("Failed to fetch unit {}: {}", unitId, e.getMessage());
            return null;
        }
    }
}

