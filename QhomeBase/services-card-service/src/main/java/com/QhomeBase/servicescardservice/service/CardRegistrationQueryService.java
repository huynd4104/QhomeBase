package com.QhomeBase.servicescardservice.service;

import com.QhomeBase.servicescardservice.dto.CardRegistrationSummaryDto;
import com.QhomeBase.servicescardservice.model.ElevatorCardRegistration;
import com.QhomeBase.servicescardservice.model.RegisterServiceRequest;
import com.QhomeBase.servicescardservice.model.ResidentCardRegistration;
import com.QhomeBase.servicescardservice.repository.ElevatorCardRegistrationRepository;
import com.QhomeBase.servicescardservice.repository.RegisterServiceRequestRepository;
import com.QhomeBase.servicescardservice.repository.ResidentCardRegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CardRegistrationQueryService {

    private static final String CARD_TYPE_RESIDENT = "RESIDENT_CARD";
    private static final String CARD_TYPE_ELEVATOR = "ELEVATOR_CARD";
    private static final String CARD_TYPE_VEHICLE = "VEHICLE_CARD";
    private static final String VEHICLE_SERVICE_TYPE = "VEHICLE_REGISTRATION";

    private final ResidentCardRegistrationRepository residentCardRepository;
    private final ElevatorCardRegistrationRepository elevatorCardRepository;
    private final RegisterServiceRequestRepository vehicleRegistrationRepository;
    private final BaseServiceClient baseServiceClient;
    
    private static final String STATUS_CANCELLED = "CANCELLED";

    @Transactional(readOnly = true)
    public List<CardRegistrationSummaryDto> getCardRegistrations(UUID userId, UUID residentId, UUID unitId) {
        List<CardRegistrationSummaryDto> items = new ArrayList<>();
        
        // Check if user is Owner of the unit (if unitId is provided)
        boolean isOwner = false;
        if (unitId != null && userId != null) {
            try {
                isOwner = baseServiceClient.isOwnerOfUnit(userId, unitId, null);
            } catch (Exception e) {
                // If check fails, assume not Owner for safety
                isOwner = false;
            }
        }

        if (residentId != null) {
            List<ResidentCardRegistration> residentCards = fetchResidentCards(userId, residentId, unitId, isOwner);
            residentCards.stream()
                    .map(this::mapResidentCard)
                    .forEach(items::add);

            List<ElevatorCardRegistration> elevatorCards = fetchElevatorCards(userId, residentId, unitId, isOwner);
            elevatorCards.stream()
                    .map(this::mapElevatorCard)
                    .forEach(items::add);
        }

        if (userId != null) {
            List<RegisterServiceRequest> vehicleCards = fetchVehicleCards(userId, unitId, isOwner);
            vehicleCards.stream()
                    .filter(request -> VEHICLE_SERVICE_TYPE.equalsIgnoreCase(request.getServiceType()))
                    .map(this::mapVehicleCard)
                    .forEach(items::add);
        }

        items.sort(Comparator.comparing(CardRegistrationSummaryDto::createdAt,
                Comparator.nullsLast(OffsetDateTime::compareTo)).reversed());
        return items;
    }

    private List<ResidentCardRegistration> fetchResidentCards(UUID userId, UUID residentId, UUID unitId, boolean isOwner) {
        if (unitId != null) {
            // If Owner, return all cards in unit. Otherwise, filter by residentId or userId
            if (isOwner) {
                return residentCardRepository.findByUnitId(unitId);
            } else {
                // Household member: only return their own cards
                if (residentId != null) {
                    return residentCardRepository.findByResidentIdAndUnitId(residentId, unitId);
                }
                if (userId != null) {
                    return residentCardRepository.findByUserIdAndUnitId(userId, unitId);
                }
                return Collections.emptyList();
            }
        }
        if (residentId != null) {
            List<ResidentCardRegistration> cards = residentCardRepository.findByResidentId(residentId);
            if (!CollectionUtils.isEmpty(cards)) {
                return cards;
            }
        }
        if (userId != null) {
            return residentCardRepository.findByUserId(userId);
        }
        return Collections.emptyList();
    }

    private List<ElevatorCardRegistration> fetchElevatorCards(UUID userId, UUID residentId, UUID unitId, boolean isOwner) {
        if (unitId != null) {
            // If Owner, return all cards in unit. Otherwise, filter by residentId or userId
            if (isOwner) {
                return elevatorCardRepository.findByUnitId(unitId);
            } else {
                // Household member: only return their own cards
                if (residentId != null) {
                    return elevatorCardRepository.findByResidentIdAndUnitId(residentId, unitId);
                }
                if (userId != null) {
                    return elevatorCardRepository.findByUserIdAndUnitId(userId, unitId);
                }
                return Collections.emptyList();
            }
        }
        if (residentId != null) {
            List<ElevatorCardRegistration> cards = elevatorCardRepository.findByResidentId(residentId);
            if (!CollectionUtils.isEmpty(cards)) {
                return cards;
            }
        }
        if (userId != null) {
            return elevatorCardRepository.findByUserId(userId);
        }
        return Collections.emptyList();
    }

    private List<RegisterServiceRequest> fetchVehicleCards(UUID userId, UUID unitId, boolean isOwner) {
        if (unitId != null) {
            // If Owner, return all vehicle cards in unit. Otherwise, only return own cards
            if (isOwner) {
                return vehicleRegistrationRepository.findByUnitId(unitId);
            } else {
                // Household member: only return their own cards
                return vehicleRegistrationRepository.findByUserIdAndUnitId(userId, unitId);
            }
        }
        // If no unitId, only return own cards
        return vehicleRegistrationRepository.findByUserId(userId);
    }

    private CardRegistrationSummaryDto mapResidentCard(ResidentCardRegistration entity) {
        return new CardRegistrationSummaryDto(
                entity.getId(),
                CARD_TYPE_RESIDENT,
                entity.getUserId(),
                entity.getResidentId(),
                entity.getUnitId(),
                normalize(entity.getRequestType()),
                normalize(entity.getStatus()),
                normalize(entity.getPaymentStatus()),
                entity.getPaymentAmount(),
                entity.getPaymentDate(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                firstNonBlank(entity.getFullName(), "Đăng ký thẻ cư dân"),
                firstNonBlank(entity.getApartmentNumber(), entity.getCitizenId()),
                entity.getApartmentNumber(),
                entity.getBuildingName(),
                entity.getNote(),
                entity.getApprovedAt(),
                entity.getVnpayInitiatedAt(),
                null // canReissue only applies to vehicle cards
        );
    }

    private CardRegistrationSummaryDto mapElevatorCard(ElevatorCardRegistration entity) {
        return new CardRegistrationSummaryDto(
                entity.getId(),
                CARD_TYPE_ELEVATOR,
                entity.getUserId(),
                entity.getResidentId(),
                entity.getUnitId(),
                normalize(entity.getRequestType()),
                normalize(entity.getStatus()),
                normalize(entity.getPaymentStatus()),
                entity.getPaymentAmount(),
                entity.getPaymentDate(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                firstNonBlank(entity.getFullName(), "Đăng ký thẻ thang máy"),
                firstNonBlank(entity.getApartmentNumber(), entity.getCitizenId()),
                entity.getApartmentNumber(),
                entity.getBuildingName(),
                entity.getNote(),
                entity.getApprovedAt(),
                entity.getVnpayInitiatedAt(),
                null // canReissue only applies to vehicle cards
        );
    }

    private CardRegistrationSummaryDto mapVehicleCard(RegisterServiceRequest entity) {
        BigDecimal amount = entity.getPaymentAmount();
        String normalizedStatus = normalize(entity.getStatus());
        String normalizedPaymentStatus = normalize(entity.getPaymentStatus());
        
        // Calculate canReissue: only if card is CANCELLED, PAID, and hasn't been reissued yet
        Boolean canReissue = null;
        if (STATUS_CANCELLED.equals(normalizedStatus) 
                && "PAID".equals(normalizedPaymentStatus)
                && entity.getReissuedFromCardId() == null) { // Not already a reissued card
            // Check if this card has already been reissued
            canReissue = !vehicleRegistrationRepository.existsReissuedCard(entity.getId());
        } else {
            canReissue = false;
        }
        
        return new CardRegistrationSummaryDto(
                entity.getId(),
                CARD_TYPE_VEHICLE,
                entity.getUserId(),
                null,
                entity.getUnitId(),
                normalize(entity.getRequestType()),
                normalizedStatus,
                normalizedPaymentStatus,
                amount,
                entity.getPaymentDate(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                firstNonBlank(entity.getLicensePlate(), "Đăng ký thẻ xe"),
                firstNonBlank(entity.getVehicleType(), entity.getBuildingName()),
                entity.getApartmentNumber(),
                entity.getBuildingName(),
                entity.getNote(),
                entity.getApprovedAt(),
                null, // Vehicle cards don't have vnpayInitiatedAt field
                canReissue
        );
    }

    private String normalize(String value) {
        return value != null ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary.trim();
        }
        return fallback;
    }
}


