package com.QhomeBase.datadocsservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "contracts", schema = "files",
        uniqueConstraints = @UniqueConstraint(name = "uq_contracts_number", columnNames = {"contract_number"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contract {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "unit_id", nullable = false)
    private UUID unitId;

    @Column(name = "contract_number", nullable = false, unique = true, length = 100)
    private String contractNumber;

    @Column(name = "contract_type", nullable = false, length = 50)
    @Builder.Default
    private String contractType = "RENTAL";

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "checkout_date")
    private LocalDate checkoutDate;

    @Column(name = "monthly_rent", precision = 14, scale = 2)
    private BigDecimal monthlyRent;

    @Column(name = "purchase_price", precision = 14, scale = 2)
    private BigDecimal purchasePrice;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "payment_terms", columnDefinition = "TEXT")
    private String paymentTerms;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "renewal_reminder_sent_at")
    private OffsetDateTime renewalReminderSentAt;

    @Column(name = "third_reminder_sent_at")
    private OffsetDateTime thirdReminderSentAt;

    @Column(name = "renewal_declined_at")
    private OffsetDateTime renewalDeclinedAt;

    @Column(name = "renewal_status", length = 20)
    @Builder.Default
    private String renewalStatus = "PENDING";

    /**
     * Track which reminder count user has dismissed
     * - 0: Not dismissed yet
     * - 1: User dismissed reminder 1 (30 days before)
     * - 2: User dismissed reminder 2 (20 days before)
     * - null/0: Show all reminders
     * Logic: Only show reminder if reminderCount > lastDismissedReminderCount
     */
    @Column(name = "last_dismissed_reminder_count")
    @Builder.Default
    private Integer lastDismissedReminderCount = 0;

    @Column(name = "renewed_contract_id")
    private UUID renewedContractId;

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, uploadedAt ASC")
    private List<ContractFile> files;
}

