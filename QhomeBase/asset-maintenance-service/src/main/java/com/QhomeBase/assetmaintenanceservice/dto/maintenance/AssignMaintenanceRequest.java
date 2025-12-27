package com.QhomeBase.assetmaintenanceservice.dto.maintenance;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignMaintenanceRequest {

    @NotNull(message = "Technician ID is required")
    private UUID technicianId;

    private String priority; // 'Normal', 'High', 'Urgent'

    private String notes;
}

