package com.QhomeBase.financebillingservice.dto;

import com.QhomeBase.financebillingservice.model.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateInvoiceStatusRequest {
    private InvoiceStatus status;
    private String reason;
}




