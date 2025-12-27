package com.QhomeBase.assetmaintenanceservice.service;

import com.QhomeBase.assetmaintenanceservice.client.BaseServiceClient;
import com.QhomeBase.assetmaintenanceservice.dto.maintenance.TechnicianWorkloadDto;
import com.QhomeBase.assetmaintenanceservice.repository.MaintenanceRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class TechnicianWorkloadService {

    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final BaseServiceClient baseServiceClient;

    public TechnicianWorkloadDto getTechnicianWorkload(UUID technicianId) {
        long pendingTasksCount = maintenanceRecordRepository.countPendingTasksByAssignedTo(technicianId);
        long assignedTasksCount = maintenanceRecordRepository.countAssignedTasksByAssignedTo(technicianId);
        long inProgressTasksCount = maintenanceRecordRepository.countInProgressTasksByAssignedTo(technicianId);

        TechnicianWorkloadDto.WorkloadLevel workloadLevel = calculateWorkloadLevel((int) pendingTasksCount);
        boolean canAssignMore = pendingTasksCount < 8;

        String technicianName = getTechnicianName(technicianId);

        return TechnicianWorkloadDto.builder()
                .technicianId(technicianId)
                .technicianName(technicianName)
                .pendingTasksCount((int) pendingTasksCount)
                .assignedTasksCount((int) assignedTasksCount)
                .inProgressTasksCount((int) inProgressTasksCount)
                .workloadLevel(workloadLevel)
                .canAssignMore(canAssignMore)
                .build();
    }

    public List<TechnicianWorkloadDto> getAllTechniciansWorkload(Boolean onlyAvailable) {
        List<Object[]> workloadStats = maintenanceRecordRepository.countWorkloadByTechnician();
        
        Map<UUID, WorkloadStats> statsMap = new HashMap<>();
        for (Object[] row : workloadStats) {
            UUID technicianId = (UUID) row[0];
            Long pendingCount = ((Number) row[1]).longValue();
            Long assignedCount = ((Number) row[2]).longValue();
            Long inProgressCount = ((Number) row[3]).longValue();
            
            statsMap.put(technicianId, new WorkloadStats(
                    pendingCount.intValue(),
                    assignedCount.intValue(),
                    inProgressCount.intValue()
            ));
        }
        
        List<UUID> technicianIds = new ArrayList<>(statsMap.keySet());
        
        if (Boolean.TRUE.equals(onlyAvailable)) {
            technicianIds = technicianIds.stream()
                    .filter(id -> statsMap.get(id).pendingCount < 8)
                    .collect(Collectors.toList());
        }
        
        Map<UUID, String> technicianNames = batchGetTechnicianNames(technicianIds);
        
        return technicianIds.stream()
                .map(technicianId -> {
                    WorkloadStats stats = statsMap.get(technicianId);
                    String technicianName = technicianNames.getOrDefault(technicianId, "Unknown Technician");
                    
                    return TechnicianWorkloadDto.builder()
                            .technicianId(technicianId)
                            .technicianName(technicianName)
                            .pendingTasksCount(stats.pendingCount)
                            .assignedTasksCount(stats.assignedCount)
                            .inProgressTasksCount(stats.inProgressCount)
                            .workloadLevel(calculateWorkloadLevel(stats.pendingCount))
                            .canAssignMore(stats.pendingCount < 8)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<TechnicianWorkloadDto> getAvailableTechnicians(UUID excludeTechnicianId, Integer maxPendingTasks) {
        int maxTasks = maxPendingTasks != null ? maxPendingTasks : 7;

        List<UUID> technicianIds = maintenanceRecordRepository.findTechniciansWithPendingTasks();
        
        if (technicianIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<Object[]> workloadStats = maintenanceRecordRepository.countWorkloadByTechnicians(technicianIds);
        
        Map<UUID, WorkloadStats> statsMap = new HashMap<>();
        for (Object[] row : workloadStats) {
            UUID technicianId = (UUID) row[0];
            Long pendingCount = ((Number) row[1]).longValue();
            Long assignedCount = ((Number) row[2]).longValue();
            Long inProgressCount = ((Number) row[3]).longValue();
            
            statsMap.put(technicianId, new WorkloadStats(
                    pendingCount.intValue(),
                    assignedCount.intValue(),
                    inProgressCount.intValue()
            ));
        }
        
        List<UUID> availableIds = technicianIds.stream()
                .filter(technicianId -> {
                    if (excludeTechnicianId != null && technicianId.equals(excludeTechnicianId)) {
                        return false;
                    }
                    WorkloadStats stats = statsMap.get(technicianId);
                    if (stats == null) {
                        return true;
                    }
                    return stats.pendingCount <= maxTasks;
                })
                .collect(Collectors.toList());
        
        Map<UUID, String> technicianNames = batchGetTechnicianNames(availableIds);
        
        return availableIds.stream()
                .map(technicianId -> {
                    WorkloadStats stats = statsMap.getOrDefault(technicianId, new WorkloadStats(0, 0, 0));
                    String technicianName = technicianNames.getOrDefault(technicianId, "Unknown Technician");
                    
                    return TechnicianWorkloadDto.builder()
                            .technicianId(technicianId)
                            .technicianName(technicianName)
                            .pendingTasksCount(stats.pendingCount)
                            .assignedTasksCount(stats.assignedCount)
                            .inProgressTasksCount(stats.inProgressCount)
                            .workloadLevel(calculateWorkloadLevel(stats.pendingCount))
                            .canAssignMore(stats.pendingCount < 8)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private String getTechnicianName(UUID technicianId) {
        try {
            return baseServiceClient.getUserName(technicianId);
        } catch (Exception e) {
            log.warn("Could not fetch technician name for ID: {}", technicianId);
            return "Unknown Technician";
        }
    }
    
    private Map<UUID, String> batchGetTechnicianNames(List<UUID> technicianIds) {
        Map<UUID, String> names = new HashMap<>();
        for (UUID technicianId : technicianIds) {
            try {
                String name = baseServiceClient.getUserName(technicianId);
                names.put(technicianId, name);
            } catch (Exception e) {
                log.warn("Could not fetch technician name for ID: {}", technicianId);
                names.put(technicianId, "Unknown Technician");
            }
        }
        return names;
    }
    
    private static class WorkloadStats {
        final int pendingCount;
        final int assignedCount;
        final int inProgressCount;
        
        WorkloadStats(int pendingCount, int assignedCount, int inProgressCount) {
            this.pendingCount = pendingCount;
            this.assignedCount = assignedCount;
            this.inProgressCount = inProgressCount;
        }
    }

    private TechnicianWorkloadDto.WorkloadLevel calculateWorkloadLevel(int pendingTasksCount) {
        if (pendingTasksCount >= 8) {
            return TechnicianWorkloadDto.WorkloadLevel.MAX_REACHED;
        } else if (pendingTasksCount >= 6) {
            return TechnicianWorkloadDto.WorkloadLevel.VERY_HIGH;
        } else if (pendingTasksCount >= 4) {
            return TechnicianWorkloadDto.WorkloadLevel.HIGH;
        } else {
            return TechnicianWorkloadDto.WorkloadLevel.NORMAL;
        }
    }
}

