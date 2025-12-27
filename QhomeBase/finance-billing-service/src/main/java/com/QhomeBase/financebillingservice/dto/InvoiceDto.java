package com.QhomeBase.financebillingservice.dto;

import com.QhomeBase.financebillingservice.model.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDto {
    private UUID id;
    private String code;
    private OffsetDateTime issuedAt;
    private LocalDate dueDate;
    private InvoiceStatus status;
    private String currency;
    private String billToName;
    private String billToAddress;
    private String billToContact;
    private UUID payerUnitId;
    private UUID payerResidentId;
    private UUID cycleId;
    private BigDecimal totalAmount;
    private List<InvoiceLineDto> lines;
    private String paymentGateway;
    private String vnpTransactionRef;
    private String vnpTransactionNo;
    private String vnpBankCode;
    private String vnpCardType;
    private String vnpResponseCode;
    private OffsetDateTime paidAt;
}







