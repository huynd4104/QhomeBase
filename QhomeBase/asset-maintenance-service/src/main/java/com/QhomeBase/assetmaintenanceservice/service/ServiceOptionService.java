package com.QhomeBase.assetmaintenanceservice.service;

import com.QhomeBase.assetmaintenanceservice.dto.service.CreateServiceOptionRequest;
import com.QhomeBase.assetmaintenanceservice.dto.service.ServiceOptionDto;
import com.QhomeBase.assetmaintenanceservice.dto.service.UpdateServiceOptionRequest;
import com.QhomeBase.assetmaintenanceservice.model.service.ServiceOption;
import com.QhomeBase.assetmaintenanceservice.repository.ServiceOptionRepository;
import com.QhomeBase.assetmaintenanceservice.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceOptionService {

    private final ServiceRepository serviceRepository;
    private final ServiceOptionRepository serviceOptionRepository;
    private final ServiceConfigService serviceConfigService;

    @Transactional(readOnly = true)
    public List<ServiceOptionDto> getOptions(UUID serviceId, Boolean isActive) {
        com.QhomeBase.assetmaintenanceservice.model.service.Service service = findServiceOrThrow(serviceId);
        return serviceOptionRepository.findAllByServiceId(service.getId()).stream()
                .filter(option -> filterByActive(option, isActive))
                .sorted(Comparator.comparing(ServiceOption::getSortOrder, Comparator.nullsFirst(Integer::compareTo))
                        .thenComparing(ServiceOption::getName, String.CASE_INSENSITIVE_ORDER))
                .map(serviceConfigService::toOptionDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ServiceOptionDto> getAllOptions(Boolean isActive) {
        return serviceOptionRepository.findAll().stream()
                .filter(option -> filterByActive(option, isActive))
                .map(serviceConfigService::toOptionDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ServiceOptionDto getOption(UUID optionId) {
        ServiceOption option = serviceOptionRepository.findById(optionId)
                .orElseThrow(() -> new IllegalArgumentException("Service option not found: " + optionId));
        return serviceConfigService.toOptionDto(option);
    }

    @Transactional
    public ServiceOptionDto createOption(UUID serviceId, CreateServiceOptionRequest request) {
        com.QhomeBase.assetmaintenanceservice.model.service.Service service = findServiceOrThrow(serviceId);
        // Allow services to configure options regardless of bookingType
        validateOptionCodeUnique(serviceId, null, request.getCode());

        ServiceOption option = new ServiceOption();
        option.setService(service);
        option.setCode(request.getCode().trim());
        option.setName(request.getName().trim());
        option.setDescription(trimToNull(request.getDescription()));
        option.setPrice(request.getPrice());
        option.setUnit(trimToNull(request.getUnit()));
        option.setIsRequired(request.getIsRequired() != null ? request.getIsRequired() : Boolean.FALSE);
        option.setIsActive(request.getIsActive() != null ? request.getIsActive() : Boolean.TRUE);
        if (Boolean.TRUE.equals(option.getIsRequired()) && Boolean.FALSE.equals(option.getIsActive())) {
            throw new IllegalArgumentException("A required option cannot be created as inactive");
        }
        option.setSortOrder(resolveOptionSortOrder(serviceId, request.getSortOrder()));

        ServiceOption saved = serviceOptionRepository.save(option);
        return serviceConfigService.toOptionDto(saved);
    }

    @Transactional
    public ServiceOptionDto updateOption(UUID optionId, UpdateServiceOptionRequest request) {
        ServiceOption option = serviceOptionRepository.findById(optionId)
                .orElseThrow(() -> new IllegalArgumentException("Service option not found: " + optionId));

        option.setName(request.getName().trim());
        option.setDescription(trimToNull(request.getDescription()));
        option.setPrice(request.getPrice());
        option.setUnit(trimToNull(request.getUnit()));
        if (request.getIsRequired() != null) {
            option.setIsRequired(request.getIsRequired());
        }
        if (request.getIsActive() != null) {
            if (Boolean.TRUE.equals(option.getIsRequired()) && Boolean.FALSE.equals(request.getIsActive())) {
                throw new IllegalArgumentException("Cannot deactivate an option that is marked as required");
            }
            option.setIsActive(request.getIsActive());
        }
        if (request.getSortOrder() != null) {
            option.setSortOrder(request.getSortOrder());
        }

        ServiceOption saved = serviceOptionRepository.save(option);
        return serviceConfigService.toOptionDto(saved);
    }

    @Transactional
    public ServiceOptionDto setOptionStatus(UUID optionId, boolean active) {
        ServiceOption option = serviceOptionRepository.findById(optionId)
                .orElseThrow(() -> new IllegalArgumentException("Service option not found: " + optionId));
        if (Boolean.TRUE.equals(option.getIsRequired()) && !active) {
            throw new IllegalArgumentException("Cannot deactivate an option that is marked as required");
        }
        option.setIsActive(active);
        ServiceOption saved = serviceOptionRepository.save(option);
        return serviceConfigService.toOptionDto(saved);
    }

    @Transactional
    public void deleteOption(UUID optionId) {
        ServiceOption option = serviceOptionRepository.findById(optionId)
                .orElseThrow(() -> new IllegalArgumentException("Service option not found: " + optionId));
        if (Boolean.TRUE.equals(option.getIsRequired())) {
            throw new IllegalArgumentException("Cannot delete an option that is marked as required");
        }
        serviceOptionRepository.delete(option);
    }

    private com.QhomeBase.assetmaintenanceservice.model.service.Service findServiceOrThrow(UUID serviceId) {
        return serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceId));
    }

    private boolean filterByActive(ServiceOption option, Boolean isActive) {
        if (isActive == null) {
            return true;
        }
        boolean optionActive = Boolean.TRUE.equals(option.getIsActive());
        return Boolean.TRUE.equals(isActive) ? optionActive : !optionActive;
    }

    private void validateOptionCodeUnique(UUID serviceId, UUID optionId, String code) {
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("Option code is required");
        }
        String trimmed = code.trim();
        boolean exists = optionId == null
                ? serviceOptionRepository.existsByServiceIdAndCodeIgnoreCase(serviceId, trimmed)
                : serviceOptionRepository.existsByServiceIdAndCodeIgnoreCaseAndIdNot(serviceId, trimmed, optionId);
        if (exists) {
            throw new IllegalArgumentException("Option code already exists for this service: " + trimmed);
        }
    }

    private int resolveOptionSortOrder(UUID serviceId, Integer requested) {
        if (requested != null) {
            return requested;
        }
        return serviceOptionRepository.findAllByServiceId(serviceId).stream()
                .map(ServiceOption::getSortOrder)
                .filter(sort -> sort != null)
                .max(Integer::compareTo)
                .map(max -> max + 1)
                .orElse(1);
    }


    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}


