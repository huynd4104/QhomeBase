package com.QhomeBase.financebillingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildingInvoiceSummaryDto {
    private UUID buildingId;
    private String buildingCode;
    private String buildingName;
    private String status;
    private BigDecimal totalAmount;
    private Long invoiceCount;
}

