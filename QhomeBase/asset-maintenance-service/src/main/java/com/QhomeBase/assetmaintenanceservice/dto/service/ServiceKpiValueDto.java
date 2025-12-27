package com.QhomeBase.assetmaintenanceservice.dto.service;

import com.QhomeBase.assetmaintenanceservice.model.service.enums.ServiceKpiValueSource;
import com.QhomeBase.assetmaintenanceservice.model.service.enums.ServiceKpiValueStatus;
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
public class ServiceKpiValueDto {

    private UUID id;
    private UUID metricId;
    private UUID serviceId;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal actualValue;
    private BigDecimal variance;
    private ServiceKpiValueStatus status;
    private ServiceKpiValueSource source;
    private OffsetDateTime recordedAt;
    private UUID recordedBy;
    private String notes;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}

