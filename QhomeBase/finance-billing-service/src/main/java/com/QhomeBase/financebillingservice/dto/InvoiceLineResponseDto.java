package com.QhomeBase.financebillingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceLineResponseDto {
    private String payerUnitId;
    private String invoiceId;
    private String invoiceCode; // ✅ Add invoice code for display
    private String serviceDate;
    private String description;
    private Double quantity;
    private String unit;
    private Double unitPrice;
    private Double taxAmount;
    private Double lineTotal;
    private Double totalAfterTax; // ✅ Total amount after tax for display
    private String serviceCode;
    private String status;
    private OffsetDateTime paidAt; // ✅ Add paid date for filtering
    private String paymentGateway; // ✅ Add payment gateway (VNPAY, CASH, etc.)
    
    // Permission fields
    private Boolean isOwner; // true if current user is OWNER or TENANT of the unit
    private Boolean canPay; // true if user can pay this invoice
    private String permissionMessage; // Message to display if user doesn't have permission
}

