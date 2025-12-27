package com.QhomeBase.assetmaintenanceservice.dto.maintenance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkCreateScheduleResponse {
    
    private Integer totalAssets;
    private Integer successfulCount;
    private Integer skippedCount;
    private Integer failedCount;
    private List<MaintenanceScheduleResponse> createdSchedules;
    private List<String> skippedAssets;
    private List<String> errors;
}



