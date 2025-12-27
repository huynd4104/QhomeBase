package com.QhomeBase.financebillingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceCategoryResponseDto {
    private String categoryCode;
    private String categoryName;
    private Double totalAmount;
    private Integer invoiceCount;
    private List<InvoiceLineResponseDto> invoices;
}

