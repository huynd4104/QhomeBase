package com.QhomeBase.assetmaintenanceservice.dto.maintenance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMaintenanceRecordRequest {

    private UUID assetId;

    private String maintenanceType;

    private LocalDate maintenanceDate;

    private LocalDate dueDate;

    private String description;

    private String completionReport;

    private String technicianReport;

    private String notes;

    private BigDecimal cost;

    private List<String> partsReplaced;

    private List<String> completionImages;

    private String status; // 'SCHEDULED', 'ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'FAILED'
}










