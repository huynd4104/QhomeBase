package com.QhomeBase.assetmaintenanceservice.dto.maintenance;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkAssignMaintenanceRequest {

    @NotEmpty(message = "Record IDs are required")
    private List<UUID> recordIds;

    @NotNull(message = "Assignment mode is required")
    private AssignmentMode mode; // 'SINGLE' - assign all to one technician, 'DISTRIBUTE' - distribute evenly

    @NotNull(message = "Technician IDs are required")
    @NotEmpty(message = "At least one technician ID is required")
    private List<UUID> technicianIds;

    private DistributionMethod distributionMethod; // 'ROUND_ROBIN', 'LOAD_BALANCE', 'MANUAL'

    public enum AssignmentMode {
        SINGLE,
        DISTRIBUTE
    }

    public enum DistributionMethod {
        ROUND_ROBIN,
        LOAD_BALANCE,
        MANUAL
    }
}










