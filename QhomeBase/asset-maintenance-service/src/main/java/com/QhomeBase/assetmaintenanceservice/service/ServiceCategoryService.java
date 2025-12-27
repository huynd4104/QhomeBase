package com.QhomeBase.assetmaintenanceservice.service;

import com.QhomeBase.assetmaintenanceservice.dto.service.ServiceCategoryDto;
import com.QhomeBase.assetmaintenanceservice.model.service.ServiceCategory;
import com.QhomeBase.assetmaintenanceservice.repository.ServiceCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceCategoryService {

    private final ServiceCategoryRepository serviceCategoryRepository;

    public ServiceCategoryDto toDto(ServiceCategory category) {
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

    @Transactional
    public ServiceCategoryDto create(ServiceCategoryDto request) {
        validateCodeUniqueness(request.getCode(), null);
        ServiceCategory entity = new ServiceCategory();
        applyFromDto(entity, request);
        if (entity.getSortOrder() == null) {
            entity.setSortOrder(calculateNextSortOrder());
        }
        if (entity.getIsActive() == null) {
            entity.setIsActive(true);
        }
        return toDto(serviceCategoryRepository.save(entity));
    }

    @Transactional
    public ServiceCategoryDto update(UUID id, ServiceCategoryDto request) {
        ServiceCategory entity = serviceCategoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Service category not found: " + id));
        validateCodeUniqueness(request.getCode(), id);
        applyFromDto(entity, request);
        if (entity.getSortOrder() == null) {
            entity.setSortOrder(calculateNextSortOrder());
        }
        return toDto(serviceCategoryRepository.save(entity));
    }

    @Transactional
    public ServiceCategoryDto setActive(UUID id, boolean active) {
        ServiceCategory entity = serviceCategoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Service category not found: " + id));
        entity.setIsActive(active);
        return toDto(serviceCategoryRepository.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        if (!serviceCategoryRepository.existsById(id)) {
            throw new IllegalArgumentException("Service category not found: " + id);
        }
        serviceCategoryRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Optional<ServiceCategoryDto> findById(UUID id) {
        return serviceCategoryRepository.findById(id).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<ServiceCategoryDto> findAll() {
        return serviceCategoryRepository.findAll(Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.asc("name"))).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private void applyFromDto(ServiceCategory entity, ServiceCategoryDto dto) {
        if (dto == null) {
            return;
        }
        if (dto.getCode() != null) {
            String code = dto.getCode().trim();
            if (code.isEmpty()) {
                throw new IllegalArgumentException("Service category code must not be blank");
            }
            entity.setCode(code);
        }
        if (dto.getName() != null) {
            String name = dto.getName().trim();
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Service category name must not be blank");
            }
            entity.setName(name);
        }
        entity.setDescription(dto.getDescription());
        entity.setIcon(dto.getIcon());
        entity.setSortOrder(dto.getSortOrder());
        if (dto.getIsActive() != null) {
            entity.setIsActive(dto.getIsActive());
        }
    }

    private int calculateNextSortOrder() {
        return serviceCategoryRepository.findTopByOrderBySortOrderDesc()
                .map(ServiceCategory::getSortOrder)
                .orElse(-1) + 1;
    }

    private void validateCodeUniqueness(String code, UUID excludeId) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Service category code is required");
        }
        boolean exists = excludeId == null
                ? serviceCategoryRepository.existsByCodeIgnoreCase(code)
                : serviceCategoryRepository.existsByCodeIgnoreCaseAndIdNot(code, excludeId);
        if (exists) {
            throw new IllegalArgumentException("Service category code already exists: " + code);
        }
    }
}