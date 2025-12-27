package com.QhomeBase.financebillingservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments", schema = "finance")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "tenant_id")
    private UUID tenantId;
    
    @Column(name = "receipt_no", nullable = false)
    private String receiptNo;
    
    @Column(name = "method", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentMethod method;
    
    @Column(name = "cash_account_id")
    private UUID cashAccountId;
    
    @Column(name = "paid_at", nullable = false)
    private OffsetDateTime paidAt;
    
    @Column(name = "amount_total", nullable = false, precision = 14, scale = 4)
    private BigDecimal amountTotal;
    
    @Column(name = "currency", nullable = false)
    private String currency;
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
    
    @Column(name = "note")
    private String note;
    
    @Column(name = "payer_resident_id")
    private UUID payerResidentId;
}




