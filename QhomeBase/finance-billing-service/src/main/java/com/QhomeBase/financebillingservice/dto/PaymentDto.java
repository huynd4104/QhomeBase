package com.QhomeBase.financebillingservice.dto;

import com.QhomeBase.financebillingservice.model.PaymentMethod;
import com.QhomeBase.financebillingservice.model.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDto {
    private UUID id;
    private String receiptNo;
    private PaymentMethod method;
    private UUID cashAccountId;
    private OffsetDateTime paidAt;
    private BigDecimal amountTotal;
    private String currency;
    private PaymentStatus status;
    private String note;
    private UUID payerResidentId;
    private List<PaymentAllocationDto> allocations;
}




