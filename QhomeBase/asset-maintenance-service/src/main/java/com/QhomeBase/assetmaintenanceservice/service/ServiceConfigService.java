package com.QhomeBase.assetmaintenanceservice.service;

import com.QhomeBase.assetmaintenanceservice.model.service.enums.ServicePricingType;
import com.QhomeBase.assetmaintenanceservice.repository.ServiceAvailabilityRepository;
import org.springframework.data.util.Pair;
import com.QhomeBase.assetmaintenanceservice.dto.service.*;
import com.QhomeBase.assetmaintenanceservice.model.service.ServiceAvailability;
import com.QhomeBase.assetmaintenanceservice.model.service.ServiceCombo;
import com.QhomeBase.assetmaintenanceservice.model.service.ServiceComboItem;
import com.QhomeBase.assetmaintenanceservice.model.service.ServiceOption;
import com.QhomeBase.assetmaintenanceservice.model.service.ServiceTicket;
import com.QhomeBase.assetmaintenanceservice.repository.ServiceCategoryRepository;
import com.QhomeBase.assetmaintenanceservice.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ServiceConfigService {

    private final ServiceRepository serviceRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;
    private final ServiceAvailabilityRepository serviceAvailabilityRepository;

    @Transactional
    public ServiceDto create(CreateServiceRequest request) {
        validateCreateRequest(request);

        var category = serviceCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Service category not found: " + request.getCategoryId()));

        if (serviceRepository.existsByCodeIgnoreCase(request.getCode())) {
            throw new IllegalArgumentException("Service code already exists: " + request.getCode());
        }

        var entity = new com.QhomeBase.assetmaintenanceservice.model.service.Service();
        entity.setCategory(category);
        entity.setCode(request.getCode().trim());
        entity.setName(request.getName().trim());
        entity.setDescription(request.getDescription());
        entity.setLocation(StringUtils.hasText(request.getLocation()) ? request.getLocation().trim() : null);
        entity.setMapUrl(StringUtils.hasText(request.getMapUrl()) ? request.getMapUrl().trim() : null);
        applyPricing(entity, request.getPricingType(), request.getPricePerHour(), request.getPricePerSession());
        entity.setMaxCapacity(request.getMaxCapacity());
        entity.setMinDurationHours(request.getMinDurationHours());
        entity.setRules(request.getRules());
        if (request.getIsActive() != null) {
            entity.setIsActive(request.getIsActive());
        }

        var saved = serviceRepository.save(entity);
        return toDto(saved);
    }
    public void resolveConfilctInCreateRequest(CreateServiceRequest request) {

    }

    @Transactional(readOnly = true)
    public ServiceDto findById(java.util.UUID id) {
        return serviceRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<ServiceDto> findAll(Boolean isActive) {
        List<com.QhomeBase.assetmaintenanceservice.model.service.Service> services =
                isActive == null ? serviceRepository.findAll() : serviceRepository.findAllByIsActive(isActive);

        return services.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(com.QhomeBase.assetmaintenanceservice.model.service.Service::getName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ServiceComboDto> findAllCombos(Boolean isActive) {
        return serviceRepository.findAll().stream()
                .flatMap(service -> streamOf(service.getCombos()))
                .map(this::mapCombo)
                .filter(combo -> matchesActive(isActive, combo.getIsActive()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ServiceOptionDto> findAllOptions(Boolean isActive) {
        return serviceRepository.findAll().stream()
                .flatMap(service -> streamOf(service.getOptions()))
                .map(this::toOptionDto)
                .filter(option -> matchesActive(isActive, option.getIsActive()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ServiceTicketDto> findAllTickets(Boolean isActive) {
        return serviceRepository.findAll().stream()
                .flatMap(service -> streamOf(service.getTickets()))
                .map(this::toTicketDto)
                .filter(ticket -> matchesActive(isActive, ticket.getIsActive()))
                .collect(Collectors.toList());
    }
    public void deactive(UUID serivceId) {
        com.QhomeBase.assetmaintenanceservice.model.service.Service service = serviceRepository.findById(serivceId).orElseThrow(()
                -> new IllegalArgumentException("Service not found: " + serivceId));
        service.setIsActive(false);
        serviceRepository.save(service);

    }

    @Transactional
    public ServiceDto update(UUID id, UpdateServiceRequest request) {
        com.QhomeBase.assetmaintenanceservice.model.service.Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + id));

        if (request.getName() != null) {
            service.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            service.setDescription(request.getDescription());
        }
        if (request.getLocation() != null) {
            service.setLocation(StringUtils.hasText(request.getLocation()) ? request.getLocation().trim() : null);
        }
        if (request.getMapUrl() != null) {
            service.setMapUrl(StringUtils.hasText(request.getMapUrl()) ? request.getMapUrl().trim() : null);
        }
        if (request.getPricingType() != null) {
            applyPricing(service, request.getPricingType(), request.getPricePerHour(), request.getPricePerSession());
        }
        if (request.getMaxCapacity() != null) {
            service.setMaxCapacity(request.getMaxCapacity());
        }
        if (request.getMinDurationHours() != null) {
            service.setMinDurationHours(request.getMinDurationHours());
        }
        if (request.getRules() != null) {
            service.setRules(request.getRules());
        }
        if (request.getIsActive() != null) {
            service.setIsActive(request.getIsActive());
        }

        service.setUpdatedAt(OffsetDateTime.now());
        var saved = serviceRepository.save(service);
        return toDto(saved);
    }

    @Transactional
    public ServiceDto setActive(UUID id, boolean active) {
        com.QhomeBase.assetmaintenanceservice.model.service.Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + id));
        service.setIsActive(active);
        service.setUpdatedAt(OffsetDateTime.now());
        var saved = serviceRepository.save(service);
        return toDto(saved);
    }
    @Transactional(readOnly = true)
    public List<ServiceAvailabilityDto> findAvailability(UUID serivceId) {
        com.QhomeBase.assetmaintenanceservice.model.service.Service service = serviceRepository.findById(serivceId).orElseThrow(()
                -> new IllegalArgumentException("Service not found: " + serivceId));
        List<ServiceAvailability> serviceAvailability = service.getAvailabilities();
        return mapAvailabilities(serviceAvailability);
    }
    @Transactional
    public List<ServiceAvailabilityDto> addAvailability(UUID serivceId, ServiceAvailabilityRequest request) {
        com.QhomeBase.assetmaintenanceservice.model.service.Service service = serviceRepository.findById(serivceId).orElseThrow(()
                -> new IllegalArgumentException("Service not found: " + serivceId));
        List<ServiceAvailability> serviceAvailability = service.getAvailabilities();
        if (!validateNewAvailability(serivceId, request)) {
            throw new IllegalArgumentException("New availability is not valid");
        };
        ServiceAvailability serviceAvailabilityToAdd = ServiceAvailability.builder()
                        .service(service)
                                .dayOfWeek(request.getDayOfWeek())
                                        .startTime(request.getStartTime())
                                                .endTime(request.getEndTime())
                                                        .isAvailable(request.getIsAvailable())
                                                                .createdAt(OffsetDateTime.now()).build();
        serviceAvailabilityRepository.save(serviceAvailabilityToAdd);
        serviceAvailability.add(serviceAvailabilityToAdd);
        service.setAvailabilities(serviceAvailability);
        serviceRepository.save(service);
        return mapAvailabilities(serviceAvailability);
    }
    public boolean validateNewAvailability(UUID serivceId, ServiceAvailabilityRequest request) {
        com.QhomeBase.assetmaintenanceservice.model.service.Service services = serviceRepository.findById(serivceId).orElseThrow(()
                -> new IllegalArgumentException("Service not found: " + serivceId));
        List<ServiceAvailability> serviceAvailability = services.getAvailabilities();
        Map<Integer, List<Pair<LocalTime, LocalTime>>> existingSlots = new HashMap<>();

        for (ServiceAvailability availability : serviceAvailability) {
            if (!existingSlots.containsKey(availability.getDayOfWeek())) {
                existingSlots.put(availability.getDayOfWeek(), new ArrayList<>());
                existingSlots.get(availability.getDayOfWeek()).add(Pair.of(availability.getStartTime(), availability.getEndTime()));

            } else {
                existingSlots.get(availability.getDayOfWeek()).add(Pair.of(availability.getStartTime(), availability.getEndTime()));
            }
        }
        Integer newDayOfWeek = request.getDayOfWeek();
        List<Pair<LocalTime, LocalTime>> existingSlotsForThisDay = existingSlots.get(newDayOfWeek);


        if (existingSlotsForThisDay == null || existingSlotsForThisDay.isEmpty()) {
            return true;
        }
        for(Pair<LocalTime, LocalTime> existingSlot: existingSlotsForThisDay) {
            LocalTime existingStart = existingSlot.getFirst();
            LocalTime existingEnd = existingSlot.getSecond();
            if (request.getStartTime().isBefore(existingEnd) && request.getEndTime().isAfter(existingStart)) {
                return false;
            }
        }
        return true;
    }
    
    @Transactional
    public ServiceAvailabilityDto updateAvailability(UUID serivceId, UUID availabilityId, ServiceAvailabilityRequest request) {
        ServiceAvailability availability = serviceAvailabilityRepository.findById(availabilityId)
                .orElseThrow(() -> new IllegalArgumentException("Availability not found: " + availabilityId));

        if (availability.getService() == null || !availability.getService().getId().equals(serivceId)) {
            throw new IllegalArgumentException("Availability does not belong to service: " + serivceId);
        }

        com.QhomeBase.assetmaintenanceservice.model.service.Service service = serviceRepository.findById(serivceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serivceId));

        // Update the availability
        availability.setDayOfWeek(request.getDayOfWeek());
        availability.setStartTime(request.getStartTime());
        availability.setEndTime(request.getEndTime());
        if (request.getIsAvailable() != null) {
            availability.setIsAvailable(request.getIsAvailable());
        }

        ServiceAvailability saved = serviceAvailabilityRepository.save(availability);
        
        return ServiceAvailabilityDto.builder()
                .id(saved.getId())
                .serviceId(saved.getService() != null ? saved.getService().getId() : null)
                .dayOfWeek(saved.getDayOfWeek())
                .startTime(saved.getStartTime())
                .endTime(saved.getEndTime())
                .isAvailable(saved.getIsAvailable())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Transactional
    public List<ServiceAvailabilityDto> deleteAvailability(UUID serivceId, UUID availabilityId) {
        ServiceAvailability availability = serviceAvailabilityRepository.findById(availabilityId)
                .orElseThrow(() -> new IllegalArgumentException("Availability not found: " + availabilityId));

        if (availability.getService() == null || !availability.getService().getId().equals(serivceId)) {
            throw new IllegalArgumentException("Availability does not belong to service: " + serivceId);
        }

        com.QhomeBase.assetmaintenanceservice.model.service.Service service = serviceRepository.findById(serivceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serivceId));

        serviceAvailabilityRepository.delete(availability);
        service.getAvailabilities().removeIf(av -> av.getId().equals(availabilityId));
        serviceRepository.save(service);

        return mapAvailabilities(service.getAvailabilities());
    }


    private void validateCreateRequest(CreateServiceRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Create service request must not be null");
        }
        if (!StringUtils.hasText(request.getCode())) {
            throw new IllegalArgumentException("Service code is required");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new IllegalArgumentException("Service name is required");
        }
        if (request.getCategoryId() == null) {
            throw new IllegalArgumentException("Service category ID is required");
        }
        ServicePricingType pricingType = request.getPricingType();
        if (pricingType == null) {
            throw new IllegalArgumentException("Service pricing type is required");
        }
        switch (pricingType) {
            case HOURLY -> {
                if (request.getPricePerHour() == null || request.getPricePerHour().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("pricePerHour must be greater than 0 for HOURLY services");
                }
            }
            case SESSION -> {
                if (request.getPricePerSession() == null || request.getPricePerSession().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("pricePerSession must be greater than 0 for SESSION services");
                }
            }
            case FREE -> {
                
            }
        }
    }

    private void applyPricing(com.QhomeBase.assetmaintenanceservice.model.service.Service entity,
                              ServicePricingType pricingType,
                              BigDecimal pricePerHour,
                              BigDecimal pricePerSession) {
        if (pricingType == null) {
            entity.setPricingType(ServicePricingType.FREE);
            entity.setPricePerHour(BigDecimal.ZERO);
            entity.setPricePerSession(BigDecimal.ZERO);
            return;
        }

        switch (pricingType) {
            case HOURLY -> {
                entity.setPricingType(ServicePricingType.HOURLY);
                entity.setPricePerHour(pricePerHour);
                entity.setPricePerSession(BigDecimal.ZERO);
            }
            case SESSION -> {
                entity.setPricingType(ServicePricingType.SESSION);
                entity.setPricePerSession(pricePerSession);
                entity.setPricePerHour(BigDecimal.ZERO);
            }
            case FREE -> {
                entity.setPricingType(ServicePricingType.FREE);
                entity.setPricePerHour(BigDecimal.ZERO);
                entity.setPricePerSession(BigDecimal.ZERO);
            }
        }
    }

    public ServiceDto toDto(com.QhomeBase.assetmaintenanceservice.model.service.Service service) {
        if (service == null) {
            return null;
        }

        return ServiceDto.builder()
                .id(service.getId())
                .categoryId(service.getCategory() != null ? service.getCategory().getId() : null)
                .category(service.getCategory() != null ? mapCategory(service.getCategory()) : null)
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
                .availabilities(mapAvailabilities(service.getAvailabilities()))
                .combos(mapCombos(service.getCombos()))
                .options(mapOptions(service.getOptions()))
                .tickets(mapTickets(service.getTickets()))
                .build();
    }

    private ServiceCategoryDto mapCategory(com.QhomeBase.assetmaintenanceservice.model.service.ServiceCategory category) {
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

    private List<ServiceAvailabilityDto> mapAvailabilities(List<ServiceAvailability> availabilities) {
        if (availabilities == null || availabilities.isEmpty()) {
            return List.of();
        }
        return availabilities.stream()
                .filter(Objects::nonNull)
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

    public ServiceComboDto toComboDto(ServiceCombo combo) {
        if (combo == null) {
            return null;
        }
        return mapCombo(combo);
    }

    private List<ServiceComboDto> mapCombos(List<ServiceCombo> combos) {
        if (combos == null || combos.isEmpty()) {
            return List.of();
        }
        return combos.stream()
                .filter(Objects::nonNull)
                .map(this::mapCombo)
                .collect(Collectors.toList());
    }

    private ServiceComboDto mapCombo(ServiceCombo combo) {
        return ServiceComboDto.builder()
                .id(combo.getId())
                .serviceId(combo.getService() != null ? combo.getService().getId() : null)
                .code(combo.getCode())
                .name(combo.getName())
                .description(combo.getDescription())
                .servicesIncluded(combo.getServicesIncluded())
                .durationMinutes(combo.getDurationMinutes())
                .price(combo.getPrice())
                .isActive(combo.getIsActive())
                .sortOrder(combo.getSortOrder())
                .createdAt(combo.getCreatedAt())
                .updatedAt(combo.getUpdatedAt())
                .items(mapComboItems(combo.getItems()))
                .build();
    }

    private List<ServiceComboItemDto> mapComboItems(List<ServiceComboItem> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        return items.stream()
                .filter(Objects::nonNull)
                .map(item -> ServiceComboItemDto.builder()
                        .id(item.getId())
                        .comboId(item.getCombo() != null ? item.getCombo().getId() : null)
                        .itemName(item.getItemName())
                        .itemDescription(item.getItemDescription())
                        .itemPrice(item.getItemPrice())
                        .itemDurationMinutes(item.getItemDurationMinutes())
                        .quantity(item.getQuantity())
                        .note(item.getNote())
                        .sortOrder(item.getSortOrder())
                        .createdAt(item.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    public ServiceOptionDto toOptionDto(ServiceOption option) {
        if (option == null) {
            return null;
        }
        return ServiceOptionDto.builder()
                .id(option.getId())
                .serviceId(option.getService() != null ? option.getService().getId() : null)
                .code(option.getCode())
                .name(option.getName())
                .description(option.getDescription())
                .price(option.getPrice())
                .unit(option.getUnit())
                .isRequired(option.getIsRequired())
                .isActive(option.getIsActive())
                .sortOrder(option.getSortOrder())
                .createdAt(option.getCreatedAt())
                .updatedAt(option.getUpdatedAt())
                .build();
    }

    private List<ServiceOptionDto> mapOptions(List<ServiceOption> options) {
        if (options == null || options.isEmpty()) {
            return List.of();
        }
        return options.stream()
                .filter(Objects::nonNull)
                .map(this::toOptionDto)
                .collect(Collectors.toList());
    }

    private List<ServiceTicketDto> mapTickets(List<ServiceTicket> tickets) {
        if (tickets == null || tickets.isEmpty()) {
            return List.of();
        }
        return tickets.stream()
                .filter(Objects::nonNull)
                .map(this::toTicketDto)
                .collect(Collectors.toList());
    }

    public ServiceTicketDto toTicketDto(ServiceTicket ticket) {
        if (ticket == null) {
            return null;
        }
        return ServiceTicketDto.builder()
                .id(ticket.getId())
                .serviceId(ticket.getService() != null ? ticket.getService().getId() : null)
                .code(ticket.getCode())
                .name(ticket.getName())
                .ticketType(ticket.getTicketType())
                .durationHours(ticket.getDurationHours())
                .price(ticket.getPrice())
                .maxPeople(ticket.getMaxPeople())
                .description(ticket.getDescription())
                .isActive(ticket.getIsActive())
                .sortOrder(ticket.getSortOrder())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }

    private boolean matchesActive(Boolean requested, Boolean actual) {
        if (requested == null) {
            return true;
        }
        boolean entityActive = Boolean.TRUE.equals(actual);
        return Boolean.TRUE.equals(requested) ? entityActive : !entityActive;
    }

    private <T> Stream<T> streamOf(List<T> items) {
        if (items == null) {
            return Stream.empty();
        }
        return items.stream().filter(Objects::nonNull);
    }
}

