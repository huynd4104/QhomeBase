package com.QhomeBase.financebillingservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "invoice_lines", schema = "billing")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceLine {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;


    @Column(name = "invoice_id", nullable = false)
    private UUID invoiceId;

    @Column(name = "service_date")
    private LocalDate serviceDate;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "quantity", nullable = false, precision = 14, scale = 4)
    private BigDecimal quantity;

    @Column(name = "unit")
    private String unit;

    @Column(name = "unit_price", nullable = false, precision = 14, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRate;

    @Column(name = "tax_amount", nullable = false, precision = 14, scale = 4)
    private BigDecimal taxAmount;

    @Column(name = "service_code")
    private String serviceCode;

    @Column(name = "external_ref_type")
    private String externalRefType;

    @Column(name = "external_ref_id")
    private UUID externalRefId;

    @Transient
    public BigDecimal getLineTotal() {
        BigDecimal subtotal = quantity.multiply(unitPrice);
        return subtotal.add(taxAmount);
    }
}