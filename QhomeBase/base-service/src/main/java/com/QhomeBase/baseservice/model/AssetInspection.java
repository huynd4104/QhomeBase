package com.QhomeBase.baseservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.JdbcTypeCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "asset_inspections", schema = "data",
       uniqueConstraints = @UniqueConstraint(name = "uq_asset_inspection_contract", columnNames = {"contract_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetInspection {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "contract_id", nullable = false, unique = true)
    private UUID contractId; // Reference to contract in data-docs-service

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @Column(name = "inspection_date", nullable = false)
    private LocalDate inspectionDate;

    @Column(name = "scheduled_date")
    private LocalDate scheduledDate; // Ngày đã hẹn để nhân viên tới kiểm tra

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private InspectionStatus status = InspectionStatus.PENDING;

    @Column(name = "inspector_name")
    private String inspectorName;

    @Column(name = "inspector_id")
    private UUID inspectorId;

    @Column(name = "inspector_notes", columnDefinition = "TEXT")
    private String inspectorNotes;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "completed_by")
    private UUID completedBy;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "total_damage_cost", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalDamageCost = BigDecimal.ZERO;

    @Column(name = "invoice_id")
    private UUID invoiceId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "inspection", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AssetInspectionItem> items;
}

