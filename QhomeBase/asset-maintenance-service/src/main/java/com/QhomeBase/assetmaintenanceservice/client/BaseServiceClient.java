package com.QhomeBase.assetmaintenanceservice.client;

import com.QhomeBase.assetmaintenanceservice.client.dto.BuildingDto;
import com.QhomeBase.assetmaintenanceservice.client.dto.UnitDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class BaseServiceClient {

    private final WebClient baseServiceWebClient;

    public BaseServiceClient(@Qualifier("baseServiceWebClient") WebClient baseServiceWebClient) {
        this.baseServiceWebClient = baseServiceWebClient;
    }

    public List<BuildingDto> getAllBuildings() {
        try {
            return baseServiceWebClient
                    .get()
                    .uri("/api/buildings")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<BuildingDto>>() {
                    })
                    .block();
        } catch (Exception e) {
            log.error("Error calling base service to get all buildings: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch buildings from base service", e);
        }
    }

    public BuildingDto getBuildingById(UUID buildingId) {
        try {
            return baseServiceWebClient
                    .get()
                    .uri("/api/buildings/{buildingId}", buildingId)
                    .retrieve()
                    .bodyToMono(BuildingDto.class)
                    .block();
        } catch (Exception e) {
            log.error("Error calling base service to get building by ID {}: {}", buildingId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch building from base service: " + buildingId, e);
        }
    }

    public List<UnitDto> getUnitsByBuildingId(UUID buildingId) {
        try {
            return baseServiceWebClient
                    .get()
                    .uri("/api/units/building/{buildingId}", buildingId)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<UnitDto>>() {
                    })
                    .block();
        } catch (Exception e) {
            log.error("Error calling base service to get units by building ID {}: {}", buildingId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch units from base service for building: " + buildingId, e);
        }
    }

    public UnitDto getUnitById(UUID unitId) {
        try {
            return baseServiceWebClient
                    .get()
                    .uri("/api/units/{unitId}", unitId)
                    .retrieve()
                    .bodyToMono(UnitDto.class)
                    .block();
        } catch (Exception e) {
            log.error("Error calling base service to get unit by ID {}: {}", unitId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch unit from base service: " + unitId, e);
        }
    }

    public List<UnitDto> getUnitsByFloor(UUID buildingId, Integer floor) {
        try {
            return baseServiceWebClient
                    .get()
                    .uri("/api/units/building/{buildingId}/floor/{floor}", buildingId, floor)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<UnitDto>>() {
                    })
                    .block();
        } catch (Exception e) {
            log.error("Error calling base service to get units by building ID {} and floor {}: {}",
                    buildingId, floor, e.getMessage(), e);
            throw new RuntimeException(
                    "Failed to fetch units from base service for building: " + buildingId + " floor: " + floor, e);
        }
    }

    public boolean buildingExists(UUID buildingId) {
        try {
            BuildingDto building = getBuildingById(buildingId);
            return building != null;
        } catch (Exception e) {
            log.warn("Building does not exist: {}", buildingId);
            return false;
        }
    }

    public boolean unitExists(UUID unitId) {
        try {
            UnitDto unit = getUnitById(unitId);
            return unit != null;
        } catch (Exception e) {
            log.warn("Unit does not exist: {}", unitId);
            return false;
        }
    }

    public boolean unitBelongsToBuilding(UUID unitId, UUID buildingId) {
        try {
            UnitDto unit = getUnitById(unitId);
            return unit != null && unit.buildingId().equals(buildingId);
        } catch (Exception e) {
            log.warn("Unit {} does not belong to building {}: {}", unitId, buildingId, e.getMessage());
            return false;
        }
    }

    public String getUserName(UUID userId) {
        try {
            return baseServiceWebClient
                    .get()
                    .uri("/api/users/{userId}/name", userId)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            log.warn("Could not fetch user name for ID {}: {}", userId, e.getMessage());
            return null;
        }
    }

    public String getUserEmail(UUID userId) {
        try {
            return baseServiceWebClient
                    .get()
                    .uri("/api/users/{userId}/email", userId)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            log.warn("Could not fetch user email for ID {}: {}", userId, e.getMessage());
            return null;
        }
    }

    public com.QhomeBase.assetmaintenanceservice.client.dto.ResidentDto getResidentByUserId(UUID userId) {
        try {
            return baseServiceWebClient
                    .get()
                    .uri("/api/residents/by-user/{userId}", userId)
                    .retrieve()
                    .bodyToMono(com.QhomeBase.assetmaintenanceservice.client.dto.ResidentDto.class)
                    .block();
        } catch (Exception e) {
            log.error("Error calling base service to get resident by user ID {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch resident from base service: " + userId, e);
        }
    }

    public java.util.List<UnitDto> getUnitsByResidentId(UUID residentId) {
        try {
            return baseServiceWebClient
                    .get()
                    .uri("/api/residents/{residentId}/units", residentId)
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<java.util.List<UnitDto>>() {
                    })
                    .block();
        } catch (Exception e) {
            log.error("Error calling base service to get units by resident ID {}: {}", residentId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch units from base service for resident: " + residentId, e);
        }
    }
}
