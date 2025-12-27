package com.QhomeBase.servicescardservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "resident_card_registration", schema = "card")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResidentCardRegistration {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "unit_id", nullable = false)
    private UUID unitId;

    @Column(name = "request_type", nullable = false)
    @Builder.Default
    private String requestType = "NEW_CARD";

    @Column(name = "resident_id", nullable = false)
    private UUID residentId;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "apartment_number")
    private String apartmentNumber;

    @Column(name = "building_name")
    private String buildingName;

    @Column(name = "citizen_id")
    private String citizenId;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "note")
    private String note;

    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "PENDING_APPROVAL";

    @Column(name = "payment_status", nullable = false)
    @Builder.Default
    private String paymentStatus = "UNPAID";

    @Column(name = "payment_amount", precision = 14, scale = 2)
    private BigDecimal paymentAmount;

    @Column(name = "payment_date")
    private OffsetDateTime paymentDate;

    @Column(name = "payment_gateway")
    private String paymentGateway;

    @Column(name = "vnpay_transaction_ref")
    private String vnpayTransactionRef;

    @Column(name = "vnpay_initiated_at")
    private OffsetDateTime vnpayInitiatedAt;

    @Column(name = "admin_note")
    private String adminNote;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}

