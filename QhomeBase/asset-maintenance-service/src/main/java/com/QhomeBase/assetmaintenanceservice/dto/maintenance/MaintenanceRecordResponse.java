package com.QhomeBase.assetmaintenanceservice.dto.maintenance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceRecordResponse {

    private UUID id;

    private UUID assetId;

    private String assetCode;

    private String assetName;

    private String maintenanceType;

    private LocalDate maintenanceDate;

    private LocalDate dueDate;

    private UUID maintenanceScheduleId;

    private String maintenanceScheduleName;

    private UUID assignedTo;

    private String assignedToName;

    private Instant assignedAt;

    private Instant startedAt;

    private String description;

    private String completionReport;

    private String technicianReport;

    private String notes;

    private BigDecimal cost;

    private List<String> partsReplaced;

    private List<String> completionImages;

    private String status;

    private Instant completedAt;

    private String createdBy;

    private Instant createdAt;

    private String updatedBy;

    private Instant updatedAt;
}










