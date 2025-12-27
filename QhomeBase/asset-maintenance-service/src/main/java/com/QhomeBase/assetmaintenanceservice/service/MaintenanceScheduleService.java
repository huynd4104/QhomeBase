package com.QhomeBase.assetmaintenanceservice.service;

import com.QhomeBase.assetmaintenanceservice.dto.maintenance.BulkCreateScheduleResponse;
import com.QhomeBase.assetmaintenanceservice.dto.maintenance.CreateMaintenanceScheduleRequest;
import com.QhomeBase.assetmaintenanceservice.dto.maintenance.MaintenanceScheduleResponse;
import com.QhomeBase.assetmaintenanceservice.dto.maintenance.UpdateMaintenanceScheduleRequest;
import com.QhomeBase.assetmaintenanceservice.model.Asset;
import com.QhomeBase.assetmaintenanceservice.model.AssetType;
import com.QhomeBase.assetmaintenanceservice.model.MaintenanceSchedule;
import com.QhomeBase.assetmaintenanceservice.model.MaintenanceType;
import com.QhomeBase.assetmaintenanceservice.repository.AssetRepository;
import com.QhomeBase.assetmaintenanceservice.repository.MaintenanceScheduleRepository;
import com.QhomeBase.assetmaintenanceservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class MaintenanceScheduleService {

    private final MaintenanceScheduleRepository maintenanceScheduleRepository;
    private final AssetRepository assetRepository;

    public MaintenanceScheduleResponse getScheduleById(UUID id) {
        MaintenanceSchedule schedule = maintenanceScheduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Maintenance schedule not found with ID: " + id));
        return toDto(schedule);
    }

    public List<MaintenanceScheduleResponse> getAllSchedules(UUID assetId, UUID assignedTo, Boolean isActive, String maintenanceType) {
        return maintenanceScheduleRepository.findWithFilters(
                        assetId,
                        assignedTo,
                        isActive,
                        maintenanceType)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<MaintenanceScheduleResponse> getSchedulesByAsset(UUID assetId) {
        List<MaintenanceSchedule> schedules = maintenanceScheduleRepository.findByAssetId(assetId);
        return schedules.stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<MaintenanceScheduleResponse> getActiveSchedulesByAsset(UUID assetId) {
        List<MaintenanceSchedule> schedules = maintenanceScheduleRepository.findByAssetIdAndIsActiveTrue(assetId);
        return schedules.stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<MaintenanceScheduleResponse> getUpcomingSchedules(Integer days, UUID assetId, UUID assignedTo) {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(days != null ? days : 30);
        
        List<MaintenanceSchedule> schedules;
        if (assetId != null) {
            schedules = maintenanceScheduleRepository.findUpcomingSchedulesByAsset(assetId, today, endDate);
        } else if (assignedTo != null) {
            schedules = maintenanceScheduleRepository.findUpcomingSchedulesByAssignedTo(assignedTo, today, endDate);
        } else {
            schedules = maintenanceScheduleRepository.findUpcomingSchedules(today, endDate);
        }
        
        return schedules.stream().map(this::toDto).collect(Collectors.toList());
    }

    public MaintenanceScheduleResponse create(CreateMaintenanceScheduleRequest request, Authentication authentication) {
        validateCreateRequest(request);
        
        var p = (UserPrincipal) authentication.getPrincipal();
        UUID userId = p.uid();
        
        Asset asset = assetRepository.findById(request.getAssetId())
                .orElseThrow(() -> new IllegalArgumentException("Asset not found with ID: " + request.getAssetId()));
        
        if (asset.getIsDeleted()) {
            throw new IllegalArgumentException("Cannot create schedule for deleted asset");
        }
        
        MaintenanceSchedule schedule = MaintenanceSchedule.builder()
                .asset(asset)
                .maintenanceType(request.getMaintenanceType())
                .name(request.getName())
                .description(request.getDescription())
                .intervalDays(request.getIntervalDays())
                .startDate(request.getStartDate())
                .assignedTo(request.getAssignedTo())
                .isActive(true)
                .createdBy(userId.toString())
                .createdAt(Instant.now())
                .updatedBy(userId.toString())
                .updatedAt(Instant.now())
                .build();
        
        schedule.calculateNextMaintenanceDate();
        
        MaintenanceSchedule savedSchedule = maintenanceScheduleRepository.save(schedule);
        return toDto(savedSchedule);
    }
    public BulkCreateScheduleResponse bulkCreate(CreateMaintenanceScheduleRequest request, AssetType assetType, Authentication authentication) {
        validateBulkCreateRequest(request);
        
        var p = (UserPrincipal) authentication.getPrincipal();
        UUID userId = p.uid();
        
       
        List<Asset> assets = assetRepository.findAllAssetByAssetType(assetType);
        
        if (assets.isEmpty()) {
            throw new IllegalArgumentException("No assets found with asset type: " + assetType);
        }
        
        List<MaintenanceScheduleResponse> createdSchedules = new ArrayList<>();
        List<String> skippedAssets = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        int successfulCount = 0;
        int skippedCount = 0;
        int failedCount = 0;
        
        for (Asset asset : assets) {
            try {

                if (asset.getIsDeleted()) {
                    skippedAssets.add(asset.getCode() + " - " + asset.getName() + " (deleted)");
                    skippedCount++;
                    continue;
                }
                

                if (maintenanceScheduleRepository.existsByAssetIdAndName(asset.getId(), request.getName())) {
                    skippedAssets.add(asset.getCode() + " - " + asset.getName() + " (schedule already exists)");
                    skippedCount++;
                    continue;
                }
                

                MaintenanceSchedule schedule = MaintenanceSchedule.builder()
                        .asset(asset)
                        .maintenanceType(request.getMaintenanceType())
                        .name(request.getName())
                        .description(request.getDescription())
                        .intervalDays(request.getIntervalDays())
                        .startDate(request.getStartDate())
                        .assignedTo(request.getAssignedTo())
                        .isActive(true)
                        .createdBy(userId.toString())
                        .createdAt(Instant.now())
                        .updatedBy(userId.toString())
                        .updatedAt(Instant.now())
                        .build();
                
                schedule.calculateNextMaintenanceDate();
                
                MaintenanceSchedule savedSchedule = maintenanceScheduleRepository.save(schedule);
                createdSchedules.add(toDto(savedSchedule));
                successfulCount++;
                
            } catch (Exception e) {
                String errorMsg = asset.getCode() + " - " + asset.getName() + ": " + e.getMessage();
                errors.add(errorMsg);
                failedCount++;
            }
        }
        
        return BulkCreateScheduleResponse.builder()
                .totalAssets(assets.size())
                .successfulCount(successfulCount)
                .skippedCount(skippedCount)
                .failedCount(failedCount)
                .createdSchedules(createdSchedules)
                .skippedAssets(skippedAssets)
                .errors(errors)
                .build();
    }

    public MaintenanceScheduleResponse update(UUID scheduleId, UpdateMaintenanceScheduleRequest request, Authentication authentication) {
        MaintenanceSchedule schedule = maintenanceScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Maintenance schedule not found with ID: " + scheduleId));
        
        validateUpdateRequest(schedule, request);
        
        var p = (UserPrincipal) authentication.getPrincipal();
        UUID userId = p.uid();
        
        if (request.getAssetId() != null && !request.getAssetId().equals(schedule.getAsset().getId())) {
            Asset asset = assetRepository.findById(request.getAssetId())
                    .orElseThrow(() -> new IllegalArgumentException("Asset not found with ID: " + request.getAssetId()));
            
            if (asset.getIsDeleted()) {
                throw new IllegalArgumentException("Cannot assign schedule to deleted asset");
            }
            
            schedule.setAsset(asset);
        }
        
        if (request.getMaintenanceType() != null) {
            schedule.setMaintenanceType(request.getMaintenanceType());
        }
        
        if (request.getName() != null) {
            schedule.setName(request.getName());
        }
        
        if (request.getDescription() != null) {
            schedule.setDescription(request.getDescription());
        }
        
        if (request.getIntervalDays() != null) {
            schedule.setIntervalDays(request.getIntervalDays());
        }
        
        if (request.getStartDate() != null) {
            schedule.setStartDate(request.getStartDate());
        }
        
        if (request.getNextMaintenanceDate() != null) {
            schedule.setNextMaintenanceDate(request.getNextMaintenanceDate());
        } else if (request.getStartDate() != null || request.getIntervalDays() != null) {
            schedule.calculateNextMaintenanceDate();
        }
        
        if (request.getAssignedTo() != null) {
            schedule.setAssignedTo(request.getAssignedTo());
        }
        
        if (request.getIsActive() != null) {
            schedule.setIsActive(request.getIsActive());
        }
        
        schedule.setUpdatedBy(userId.toString());
        schedule.setUpdatedAt(Instant.now());
        
        MaintenanceSchedule updatedSchedule = maintenanceScheduleRepository.save(schedule);
        return toDto(updatedSchedule);
    }

    public void delete(UUID scheduleId, Authentication authentication) {
        MaintenanceSchedule schedule = maintenanceScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Maintenance schedule not found with ID: " + scheduleId));
        
        var p = (UserPrincipal) authentication.getPrincipal();
        UUID userId = p.uid();
        
        schedule.setIsActive(false);
        schedule.setUpdatedBy(userId.toString());
        schedule.setUpdatedAt(Instant.now());
        
        maintenanceScheduleRepository.save(schedule);
    }

    public MaintenanceScheduleResponse toggleActive(UUID scheduleId, Boolean isActive, Authentication authentication) {
        MaintenanceSchedule schedule = maintenanceScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Maintenance schedule not found with ID: " + scheduleId));
        
        var p = (UserPrincipal) authentication.getPrincipal();
        UUID userId = p.uid();
        
        schedule.setIsActive(isActive != null ? isActive : !schedule.getIsActive());
        schedule.setUpdatedBy(userId.toString());
        schedule.setUpdatedAt(Instant.now());
        
        MaintenanceSchedule updatedSchedule = maintenanceScheduleRepository.save(schedule);
        return toDto(updatedSchedule);
    }

    public MaintenanceScheduleResponse toDto(MaintenanceSchedule schedule) {
        return MaintenanceScheduleResponse.builder()
                .id(schedule.getId())
                .assetId(schedule.getAsset().getId())
                .assetCode(schedule.getAsset().getCode())
                .assetName(schedule.getAsset().getName())
                .maintenanceType(schedule.getMaintenanceType())
                .name(schedule.getName())
                .description(schedule.getDescription())
                .intervalDays(schedule.getIntervalDays())
                .startDate(schedule.getStartDate())
                .nextMaintenanceDate(schedule.getNextMaintenanceDate())
                .assignedTo(schedule.getAssignedTo())
                .assignedToName(null)
                .isActive(schedule.getIsActive())
                .createdBy(schedule.getCreatedBy())
                .createdAt(schedule.getCreatedAt())
                .updatedBy(schedule.getUpdatedBy())
                .updatedAt(schedule.getUpdatedAt())
                .build();
    }

    private void validateCreateRequest(CreateMaintenanceScheduleRequest request) {
        try {
            MaintenanceType.valueOf(request.getMaintenanceType());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid maintenance type: " + request.getMaintenanceType());
        }
        
        if (request.getStartDate() == null) {
            throw new IllegalArgumentException("Start date is required");
        }
        
        if (request.getIntervalDays() == null || request.getIntervalDays() <= 0) {
            throw new IllegalArgumentException("Interval days must be positive");
        }
        
        if (maintenanceScheduleRepository.existsByAssetIdAndName(request.getAssetId(), request.getName())) {
            throw new IllegalArgumentException("Schedule name already exists for this asset: " + request.getName());
        }
        
        LocalDate now = LocalDate.now();
        if (request.getStartDate().isBefore(now.minusDays(1))) {
            throw new IllegalArgumentException("Start date cannot be more than 1 day in the past");
        }
    }
    
    private void validateBulkCreateRequest(CreateMaintenanceScheduleRequest request) {
        try {
            MaintenanceType.valueOf(request.getMaintenanceType());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid maintenance type: " + request.getMaintenanceType());
        }
        
        if (request.getStartDate() == null) {
            throw new IllegalArgumentException("Start date is required");
        }
        
        if (request.getIntervalDays() == null || request.getIntervalDays() <= 0) {
            throw new IllegalArgumentException("Interval days must be positive");
        }
        
        LocalDate now = LocalDate.now();
        if (request.getStartDate().isBefore(now.minusDays(1))) {
            throw new IllegalArgumentException("Start date cannot be more than 1 day in the past");
        }
        
        // Note: We don't validate assetId existence here because we're creating for multiple assets
        // We don't validate duplicate schedule name here because we check it per asset in the loop
    }

    private void validateUpdateRequest(MaintenanceSchedule schedule, UpdateMaintenanceScheduleRequest request) {
        if (request.getMaintenanceType() != null) {
            try {
                MaintenanceType.valueOf(request.getMaintenanceType());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid maintenance type: " + request.getMaintenanceType());
            }
        }
        
        if (request.getIntervalDays() != null && request.getIntervalDays() <= 0) {
            throw new IllegalArgumentException("Interval days must be positive");
        }
        
        UUID assetId = request.getAssetId() != null ? request.getAssetId() : schedule.getAsset().getId();
        String name = request.getName() != null ? request.getName() : schedule.getName();
        
        if (request.getName() != null && !request.getName().equals(schedule.getName())) {
            if (maintenanceScheduleRepository.existsByAssetIdAndName(assetId, name, schedule.getId())) {
                throw new IllegalArgumentException("Schedule name already exists for this asset: " + name);
            }
        }
        
        if (request.getStartDate() != null) {
            LocalDate now = LocalDate.now();
            if (request.getStartDate().isBefore(now.minusDays(1))) {
                throw new IllegalArgumentException("Start date cannot be more than 1 day in the past");
            }
        }
        
        if (request.getNextMaintenanceDate() != null && request.getStartDate() != null) {
            if (request.getNextMaintenanceDate().isBefore(request.getStartDate())) {
                throw new IllegalArgumentException("Next maintenance date cannot be before start date");
            }
        }
        
        if (request.getNextMaintenanceDate() != null && schedule.getStartDate() != null) {
            if (request.getNextMaintenanceDate().isBefore(schedule.getStartDate())) {
                throw new IllegalArgumentException("Next maintenance date cannot be before start date");
            }
        }
    }
}
