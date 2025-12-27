package com.QhomeBase.assetmaintenanceservice.dto.maintenance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMaintenanceRecordRequest {

    @NotNull(message = "Asset ID is required")
    private UUID assetId;

    @NotBlank(message = "Maintenance type is required")
    private String maintenanceType; // 'ROUTINE', 'REPAIR', 'INSPECTION', 'EMERGENCY', 'UPGRADE'

    @NotNull(message = "Maintenance date is required")
    private LocalDate maintenanceDate;

    private LocalDate dueDate;

    private UUID maintenanceScheduleId; // Optional - if created from schedule

    private String description;
}

