package com.QhomeBase.financebillingservice.dto;

import com.QhomeBase.financebillingservice.model.AllocationType;
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
public class PaymentAllocationDto {
    private UUID id;
    private AllocationType allocationType;
    private UUID invoiceId;
    private UUID invoiceLineId;
    private BigDecimal amount;
}




