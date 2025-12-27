package com.QhomeBase.assetmaintenanceservice.dto.maintenance;

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
public class UpdateMaintenanceScheduleRequest {

    private UUID assetId;

    private String maintenanceType;

    private String name;

    private String description;

    @Positive(message = "Interval days must be positive")
    private Integer intervalDays;

    private LocalDate startDate;

    private LocalDate nextMaintenanceDate;

    private UUID assignedTo;

    private Boolean isActive;
}










