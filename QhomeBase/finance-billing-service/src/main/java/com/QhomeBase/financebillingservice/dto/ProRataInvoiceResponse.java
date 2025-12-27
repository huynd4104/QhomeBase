package com.QhomeBase.financebillingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProRataInvoiceResponse {
    private UUID invoiceId;
    private UUID vehicleId;
    private UUID unitId;
    private String plateNo;
    private BigDecimal amount;
    private Integer daysInMonth;
    private Integer chargedDays;
    private OffsetDateTime periodFrom;
    private OffsetDateTime periodTo;
    private String status;
    private String message;
}
