package com.QhomeBase.assetmaintenanceservice.dto.maintenance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceScheduleResponse {

    private UUID id;

    private UUID assetId;

    private String assetCode;

    private String assetName;

    private String maintenanceType;

    private String name;

    private String description;

    private Integer intervalDays;

    private LocalDate startDate;

    private LocalDate nextMaintenanceDate;

    private UUID assignedTo;

    private String assignedToName;

    private Boolean isActive;

    private String createdBy;

    private Instant createdAt;

    private String updatedBy;

    private Instant updatedAt;
}










