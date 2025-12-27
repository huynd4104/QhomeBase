package com.QhomeBase.baseservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "household_member_requests", schema = "data")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HouseholdMemberRequest {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "household_id", nullable = false)
    private UUID householdId;

    @Column(name = "resident_id")
    private UUID residentId;

    @Column(name = "requested_by", nullable = false)
    private UUID requestedBy;

    @Column(name = "resident_full_name", nullable = false)
    private String residentFullName;

    @Column(name = "resident_phone")
    private String residentPhone;

    @Column(name = "resident_email")
    private String residentEmail;

    @Column(name = "resident_national_id")
    private String residentNationalId;

    @Column(name = "resident_dob")
    private java.time.LocalDate residentDob;

    @Column(name = "relation")
    private String relation;

    @Column(name = "proof_of_relation_image_url", columnDefinition = "TEXT")
    private String proofOfRelationImageUrl;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private RequestStatus status = RequestStatus.PENDING;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "rejected_by")
    private UUID rejectedBy;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "rejected_at")
    private OffsetDateTime rejectedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public enum RequestStatus {
        PENDING,
        APPROVED,
        REJECTED,
        CANCELLED
    }
}
