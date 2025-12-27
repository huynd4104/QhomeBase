package com.QhomeBase.financebillingservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "payment_allocations", schema = "finance")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentAllocation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "tenant_id")
    private UUID tenantId;
    
    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;
    
    @Column(name = "allocation_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private AllocationType allocationType;
    
    @Column(name = "invoice_id")
    private UUID invoiceId;
    
    @Column(name = "invoice_line_id")
    private UUID invoiceLineId;
    
    @Column(name = "amount", nullable = false, precision = 14, scale = 4)
    private BigDecimal amount;
}




