package com.QhomeBase.assetmaintenanceservice.dto.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceKpiTargetDto {

    private UUID id;
    private UUID metricId;
    private UUID serviceId;
    private LocalDate targetPeriodStart;
    private LocalDate targetPeriodEnd;
    private BigDecimal targetValue;
    private BigDecimal thresholdWarning;
    private BigDecimal thresholdCritical;
    private UUID assignedTo;
    private String notes;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}

