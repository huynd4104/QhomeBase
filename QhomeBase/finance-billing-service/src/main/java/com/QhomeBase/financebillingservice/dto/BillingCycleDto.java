package com.QhomeBase.financebillingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingCycleDto {
    private UUID id;
    private String name;
    private LocalDate periodFrom;
    private LocalDate periodTo;
    private String status;
    private UUID externalCycleId;
    private UUID serviceId;
    private String serviceCode;
    private String serviceName;
}