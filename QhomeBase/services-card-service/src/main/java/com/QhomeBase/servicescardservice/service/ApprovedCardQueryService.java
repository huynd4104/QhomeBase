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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApprovedCardQueryService {

    private static final String CARD_TYPE_RESIDENT = "RESIDENT_CARD";
    private static final String CARD_TYPE_ELEVATOR = "ELEVATOR_CARD";
    private static final String CARD_TYPE_VEHICLE = "VEHICLE_CARD";
    private static final String VEHICLE_SERVICE_TYPE = "VEHICLE_REGISTRATION";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_COMPLETED = "COMPLETED";

    private final ResidentCardRegistrationRepository residentCardRepository;
    private final ElevatorCardRegistrationRepository elevatorCardRepository;
    private final RegisterServiceRequestRepository vehicleRegistrationRepository;
    
    private static final String STATUS_CANCELLED = "CANCELLED";

    @Transactional(readOnly = true)
    public List<CardRegistrationSummaryDto> getApprovedCards(UUID buildingId, UUID unitId) {
        List<CardRegistrationSummaryDto> items = new ArrayList<>();

        List<String> approvedStatuses = List.of(STATUS_APPROVED, STATUS_COMPLETED);

        for (String status : approvedStatuses) {
            List<ResidentCardRegistration> residentCards = residentCardRepository
                    .findApprovedCardsByBuildingAndUnit(buildingId, unitId, status);
            residentCards.stream()
                    .map(this::mapResidentCard)
                    .forEach(items::add);

            List<ElevatorCardRegistration> elevatorCards = elevatorCardRepository
                    .findApprovedCardsByBuildingAndUnit(buildingId, unitId, status);
            elevatorCards.stream()
                    .map(this::mapElevatorCard)
                    .forEach(items::add);

            List<RegisterServiceRequest> vehicleCards = vehicleRegistrationRepository
                    .findApprovedVehicleCardsByBuildingAndUnit(buildingId, unitId, status);
            vehicleCards.stream()
                    .filter(request -> VEHICLE_SERVICE_TYPE.equalsIgnoreCase(request.getServiceType()))
                    .map(this::mapVehicleCard)
                    .forEach(items::add);
        }

        items.sort(Comparator.comparing(CardRegistrationSummaryDto::approvedAt,
                Comparator.nullsLast(OffsetDateTime::compareTo)).reversed()
                .thenComparing(CardRegistrationSummaryDto::createdAt,
                        Comparator.nullsLast(OffsetDateTime::compareTo)).reversed());
        return items;
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
                entity.getVnpayInitiatedAt(),
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




