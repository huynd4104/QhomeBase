package com.QhomeBase.baseservice.model;

import com.QhomeBase.baseservice.util.StringListConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "maintenance_requests", schema = "data")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceRequest {

    @Id
    private UUID id;

    @Column(name = "unit_id", nullable = false)
    private UUID unitId;

    @Column(name = "resident_id")
    private UUID residentId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "category", nullable = false)
    private String category;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Convert(converter = StringListConverter.class)
    @Column(name = "attachments", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> attachments = new ArrayList<>();

    @Column(name = "location", nullable = false)
    private String location;

    @Column(name = "preferred_datetime")
    private OffsetDateTime preferredDatetime;

    @Column(name = "contact_name", nullable = false)
    private String contactName;

    @Column(name = "contact_phone", nullable = false)
    private String contactPhone;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "note")
    private String note;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "last_resent_at")
    private OffsetDateTime lastResentAt;

    @Column(name = "resend_alert_sent", nullable = false)
    @Builder.Default
    private boolean resendAlertSent = false;

    @Column(name = "call_alert_sent", nullable = false)
    @Builder.Default
    private boolean callAlertSent = false;

    @Column(name = "admin_response", columnDefinition = "TEXT")
    private String adminResponse;

    @Column(name = "estimated_cost", precision = 15, scale = 2)
    private java.math.BigDecimal estimatedCost;

    @Column(name = "responded_by")
    private UUID respondedBy;

    @Column(name = "responded_at")
    private OffsetDateTime respondedAt;

    @Column(name = "response_status", length = 50)
    private String responseStatus;

    @Column(name = "progress_notes", columnDefinition = "TEXT")
    private String progressNotes;

    @Column(name = "payment_status", length = 50)
    @Builder.Default
    private String paymentStatus = "UNPAID";

    @Column(name = "payment_amount", precision = 15, scale = 2)
    private java.math.BigDecimal paymentAmount;

    @Column(name = "payment_date")
    private OffsetDateTime paymentDate;

    @Column(name = "payment_gateway", length = 50)
    private String paymentGateway;

    @Column(name = "vnpay_transaction_ref", length = 255)
    private String vnpayTransactionRef;

    @PrePersist
    public void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}

