package com.QhomeBase.assetmaintenanceservice.service;

import com.QhomeBase.assetmaintenanceservice.dto.service.*;
import com.QhomeBase.assetmaintenanceservice.model.service.ServiceAvailability;
import com.QhomeBase.assetmaintenanceservice.model.service.ServiceCategory;
import com.QhomeBase.assetmaintenanceservice.model.service.ServiceCombo;
import com.QhomeBase.assetmaintenanceservice.model.service.ServiceOption;
import com.QhomeBase.assetmaintenanceservice.model.service.ServiceTicket;
import com.QhomeBase.assetmaintenanceservice.model.service.ServiceBooking;
import com.QhomeBase.assetmaintenanceservice.model.service.enums.ServiceBookingStatus;
import com.QhomeBase.assetmaintenanceservice.repository.ServiceBookingSlotRepository;
import com.QhomeBase.assetmaintenanceservice.repository.ServiceCategoryRepository;
import com.QhomeBase.assetmaintenanceservice.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@SuppressWarnings({"NullAway", "DataFlowIssue"})
public class ResidentServiceCatalogService {

    private final ServiceCategoryRepository categoryRepository;
    private final ServiceRepository serviceRepository;
    private final ServiceConfigService serviceConfigService;
    private final ServiceBookingSlotRepository serviceBookingSlotRepository;

    public List<ServiceCategoryDto> getActiveCategories() {
        return categoryRepository.findByIsActiveTrueOrderBySortOrderAscNameAsc()
                .stream()
                .map(this::toCategoryDto)
                .toList();
    }

    public List<ServiceBookedSlotDto> getBookedSlots(UUID serviceId, LocalDate fromDate, LocalDate toDate, UUID userId) {
        java.util.Objects.requireNonNull(serviceId, "Service ID is required");

        com.QhomeBase.assetmaintenanceservice.model.service.Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceId));

        if (!Boolean.TRUE.equals(service.getIsActive())) {
            throw new IllegalArgumentException("Service is inactive: " + serviceId);
        }

        LocalDate start = fromDate != null ? fromDate : LocalDate.now();
        LocalDate end = toDate != null ? toDate : start.plusDays(30);

        if (end.isBefore(start)) {
            throw new IllegalArgumentException("The `to` date must be after `from` date");
        }

        if (end.isAfter(start.plusDays(90))) {
            end = start.plusDays(90);
        }

        return serviceBookingSlotRepository
                .findAllByServiceIdAndSlotDateBetweenOrderBySlotDateAscStartTimeAsc(serviceId, start, end)
                .stream()
                .filter(slot -> slot.getBooking() != null && isVisibleStatus(slot.getBooking()))
                .filter(slot -> userId == null || slot.getBooking().getUserId().equals(userId))
                .map(slot -> ServiceBookedSlotDto.builder()
                        .bookingId(slot.getBooking().getId())
                        .slotDate(slot.getSlotDate())
                        .startTime(slot.getStartTime())
                        .endTime(slot.getEndTime())
                        .bookingStatus(slot.getBooking().getStatus() != null
                                ? slot.getBooking().getStatus().name()
                                : null)
                        .build())
                .collect(Collectors.toList());
    }

    private boolean isVisibleStatus(ServiceBooking booking) {
        ServiceBookingStatus status = booking.getStatus();
        return status == ServiceBookingStatus.PENDING
                || status == ServiceBookingStatus.APPROVED
                || status == ServiceBookingStatus.COMPLETED;
    }

    @SuppressWarnings("NullAway")
    public List<ResidentServiceSummaryDto> getActiveServicesByCategoryCode(String categoryCode) {
        if (!StringUtils.hasText(categoryCode)) {
            throw new IllegalArgumentException("Category code is required");
        }
        ServiceCategory category = categoryRepository.findByCodeIgnoreCaseAndIsActiveTrue(categoryCode)
                .orElseThrow(() -> new IllegalArgumentException("Service category not found or inactive: " + categoryCode));

        return serviceRepository.findByCategory_CodeIgnoreCaseAndIsActiveTrueOrderByNameAsc(categoryCode)
                .stream()
                .map(service -> toServiceSummaryDto(service, category))
                .toList();
    }

    public ResidentServiceDetailDto getServiceDetail(UUID serviceId) {
        java.util.Objects.requireNonNull(serviceId, "Service ID is required");
        com.QhomeBase.assetmaintenanceservice.model.service.Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceId));

        if (!Boolean.TRUE.equals(service.getIsActive())) {
            throw new IllegalArgumentException("Service is inactive: " + serviceId);
        }

        ServiceCategory category = service.getCategory();
        return ResidentServiceDetailDto.builder()
                .id(service.getId())
                .code(service.getCode())
                .name(service.getName())
                .description(service.getDescription())
                .location(service.getLocation())
                .mapUrl(service.getMapUrl())
                .pricePerHour(service.getPricePerHour())
                .pricePerSession(service.getPricePerSession())
                .pricingType(service.getPricingType())
                .maxCapacity(service.getMaxCapacity())
                .minDurationHours(service.getMinDurationHours())
                .rules(service.getRules())
                .isActive(service.getIsActive())
                .createdAt(service.getCreatedAt())
                .updatedAt(service.getUpdatedAt())
                .category(category != null ? toCategoryDto(category) : null)
                .availabilities(toAvailabilityDtos(service.getAvailabilities()))
                .combos(toComboDtos(service.getCombos()))
                .options(toOptionDtos(service.getOptions()))
                .tickets(toTicketDtos(service.getTickets()))
                .build();
    }

    private ServiceCategoryDto toCategoryDto(ServiceCategory category) {
        if (category == null) {
            return null;
        }
        return ServiceCategoryDto.builder()
                .id(category.getId())
                .code(category.getCode())
                .name(category.getName())
                .description(category.getDescription())
                .icon(category.getIcon())
                .sortOrder(category.getSortOrder())
                .isActive(category.getIsActive())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

    private ResidentServiceSummaryDto toServiceSummaryDto(
            com.QhomeBase.assetmaintenanceservice.model.service.Service service,
            ServiceCategory category) {
        return ResidentServiceSummaryDto.builder()
                .id(service.getId())
                .code(service.getCode())
                .name(service.getName())
                .description(service.getDescription())
                .location(service.getLocation())
                .mapUrl(service.getMapUrl())
                .pricePerHour(service.getPricePerHour())
                .pricePerSession(service.getPricePerSession())
                .pricingType(service.getPricingType())
                .maxCapacity(service.getMaxCapacity())
                .minDurationHours(service.getMinDurationHours())
                .rules(service.getRules())
                .categoryCode(category != null ? category.getCode() : null)
                .categoryName(category != null ? category.getName() : null)
                .build();
    }

    private List<ServiceAvailabilityDto> toAvailabilityDtos(List<ServiceAvailability> availabilities) {
        if (availabilities == null) {
            return List.of();
        }
        return availabilities.stream()
                .filter(availability -> availability != null && Boolean.TRUE.equals(availability.getIsAvailable()))
                .sorted(Comparator.comparing(ServiceAvailability::getDayOfWeek)
                        .thenComparing(ServiceAvailability::getStartTime))
                .map(availability -> ServiceAvailabilityDto.builder()
                        .id(availability.getId())
                        .serviceId(availability.getService() != null ? availability.getService().getId() : null)
                        .dayOfWeek(availability.getDayOfWeek())
                        .startTime(availability.getStartTime())
                        .endTime(availability.getEndTime())
                        .isAvailable(availability.getIsAvailable())
                        .createdAt(availability.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    private List<ServiceComboDto> toComboDtos(List<ServiceCombo> combos) {
        if (combos == null) {
            return List.of();
        }
        return combos.stream()
                .filter(combo -> combo != null && Boolean.TRUE.equals(combo.getIsActive()))
                .sorted(Comparator.comparing(ServiceCombo::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ServiceCombo::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(serviceConfigService::toComboDto)
                .collect(Collectors.toList());
    }

    private List<ServiceOptionDto> toOptionDtos(List<ServiceOption> options) {
        if (options == null) {
            return List.of();
        }
        return options.stream()
                .filter(option -> option != null && Boolean.TRUE.equals(option.getIsActive()))
                .sorted(Comparator.comparing(ServiceOption::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ServiceOption::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(serviceConfigService::toOptionDto)
                .collect(Collectors.toList());
    }

    private List<ServiceTicketDto> toTicketDtos(List<ServiceTicket> tickets) {
        if (tickets == null) {
            return List.of();
        }
        return tickets.stream()
                .filter(ticket -> ticket != null && Boolean.TRUE.equals(ticket.getIsActive()))
                .sorted(Comparator.comparing(ServiceTicket::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ServiceTicket::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(serviceConfigService::toTicketDto)
                .collect(Collectors.toList());
    }
}

