package com.QhomeBase.assetmaintenanceservice.dto.maintenance;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteMaintenanceRequest {

    @NotBlank(message = "Completion report is required")
    private String completionReport;

    private String technicianReport;

    private String issuesFound;

    private BigDecimal cost;

    private List<String> partsReplaced;

    private List<String> beforeImages;

    private List<String> afterImages;

    private String notes;
}










