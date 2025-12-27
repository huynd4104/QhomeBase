package com.QhomeBase.financebillingservice.dto;

import com.QhomeBase.financebillingservice.model.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequest {
    private PaymentMethod method;
    private UUID cashAccountId;
    private BigDecimal amountTotal;
    private String currency;
    private String note;
    private UUID payerResidentId;
    private List<PaymentAllocationDto> allocations;
}




