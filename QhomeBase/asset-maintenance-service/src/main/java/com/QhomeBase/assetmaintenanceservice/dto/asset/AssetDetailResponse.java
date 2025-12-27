package com.QhomeBase.assetmaintenanceservice.dto.asset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetDetailResponse {

    private AssetResponse asset;

    private List<MaintenanceScheduleSummaryDto> maintenanceSchedules;

    private List<MaintenanceRecordSummaryDto> recentMaintenanceRecords;

    // Helper DTOs for nested data
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MaintenanceScheduleSummaryDto {
        private java.util.UUID id;
        private String name;
        private String maintenanceType;
        private java.time.LocalDate nextMaintenanceDate;
        private Boolean isActive;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MaintenanceRecordSummaryDto {
        private java.util.UUID id;
        private java.time.LocalDate maintenanceDate;
        private java.time.LocalDate dueDate;
        private String maintenanceType;
        private String status;
        private String assignedToName;
    }
}










