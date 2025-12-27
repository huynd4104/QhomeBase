package com.QhomeBase.assetmaintenanceservice.model.service;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "service_kpi_target", schema = "asset",
       uniqueConstraints = @UniqueConstraint(columnNames = {"metric_id", "target_period_start", "target_period_end"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceKpiTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metric_id", nullable = false)
    private ServiceKpiMetric metric;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private Service service;

    @Column(name = "target_period_start", nullable = false)
    private LocalDate targetPeriodStart;

    @Column(name = "target_period_end", nullable = false)
    private LocalDate targetPeriodEnd;

    @Column(name = "target_value", precision = 15, scale = 2)
    private BigDecimal targetValue;

    @Column(name = "threshold_warning", precision = 15, scale = 2)
    private BigDecimal thresholdWarning;

    @Column(name = "threshold_critical", precision = 15, scale = 2)
    private BigDecimal thresholdCritical;

    @Column(name = "assigned_to", columnDefinition = "uuid")
    private UUID assignedTo;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}

