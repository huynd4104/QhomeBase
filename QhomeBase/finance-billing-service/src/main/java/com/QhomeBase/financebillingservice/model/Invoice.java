package com.QhomeBase.financebillingservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "invoices", schema = "billing")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "issued_at", nullable = false)
    private OffsetDateTime issuedAt;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "billing.inv_status")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.NAMED_ENUM)
    private InvoiceStatus status;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "bill_to_name")
    private String billToName;

    @Column(name = "bill_to_address")
    private String billToAddress;

    @Column(name = "bill_to_contact")
    private String billToContact;

    @Column(name = "payer_unit_id")
    private UUID payerUnitId;

    @Column(name = "payer_resident_id")
    private UUID payerResidentId;

    @Column(name = "cycle_id")
    private UUID cycleId;

    @Column(name = "payment_gateway")
    private String paymentGateway;

    @Column(name = "vnp_transaction_ref")
    private String vnpTransactionRef;

    @Column(name = "vnp_transaction_no")
    private String vnpTransactionNo;

    @Column(name = "vnp_bank_code")
    private String vnpBankCode;

    @Column(name = "vnp_card_type")
    private String vnpCardType;

    @Column(name = "vnp_response_code")
    private String vnpResponseCode;

    @Column(name = "vnpay_initiated_at")
    private OffsetDateTime vnpayInitiatedAt;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Column(name = "reminder_count", nullable = false)
    @Builder.Default
    private Integer reminderCount = 0;

    @Column(name = "last_reminder_at")
    private OffsetDateTime lastReminderAt;
}