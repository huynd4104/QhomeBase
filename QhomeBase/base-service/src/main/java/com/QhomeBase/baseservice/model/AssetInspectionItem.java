package com.QhomeBase.baseservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "asset_inspection_items", schema = "data")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetInspectionItem {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspection_id", nullable = false)
    private AssetInspection inspection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(name = "condition_status")
    private String conditionStatus; // GOOD, DAMAGED, MISSING, etc.

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "checked", nullable = false)
    @Builder.Default
    private Boolean checked = false;

    @Column(name = "checked_at")
    private OffsetDateTime checkedAt;

    @Column(name = "checked_by")
    private UUID checkedBy;

    @Column(name = "damage_cost", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal damageCost = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}

