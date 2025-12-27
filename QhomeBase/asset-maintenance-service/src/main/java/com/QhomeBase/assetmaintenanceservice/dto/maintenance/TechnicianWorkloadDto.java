package com.QhomeBase.assetmaintenanceservice.dto.maintenance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnicianWorkloadDto {

    private UUID technicianId;

    private String technicianName;

    private Integer pendingTasksCount; // ASSIGNED + IN_PROGRESS

    private Integer assignedTasksCount;

    private Integer inProgressTasksCount;

    private WorkloadLevel workloadLevel;

    private Boolean canAssignMore; // true if pendingTasksCount < 8

    public enum WorkloadLevel {
        NORMAL,      // 0-3 tasks
        HIGH,       // 4-5 tasks
        VERY_HIGH,  // 6-7 tasks
        MAX_REACHED // 8 tasks
    }
}


