package com.QhomeBase.assetmaintenanceservice.dto.maintenance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class CreateMaintenanceScheduleRequest {

    @NotNull(message = "Asset ID is required")
    private UUID assetId;

    @NotBlank(message = "Maintenance type is required")
    private String maintenanceType; // 'ROUTINE', 'REPAIR', 'INSPECTION', 'EMERGENCY', 'UPGRADE'

    @NotBlank(message = "Schedule name is required")
    private String name;

    private String description;

    @NotNull(message = "Interval days is required")
    @Positive(message = "Interval days must be positive")
    private Integer intervalDays;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    private UUID assignedTo; // Optional - default TECHNICIAN staff/user ID
}










