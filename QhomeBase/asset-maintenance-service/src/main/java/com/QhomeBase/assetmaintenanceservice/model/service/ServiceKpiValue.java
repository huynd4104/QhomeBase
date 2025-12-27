package com.QhomeBase.assetmaintenanceservice.model.service;

import com.QhomeBase.assetmaintenanceservice.model.service.enums.ServiceKpiValueSource;
import com.QhomeBase.assetmaintenanceservice.model.service.enums.ServiceKpiValueStatus;
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
@Table(name = "service_kpi_value", schema = "asset",
       uniqueConstraints = @UniqueConstraint(columnNames = {"metric_id", "period_start", "period_end"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceKpiValue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metric_id", nullable = false)
    private ServiceKpiMetric metric;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private Service service;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "actual_value", precision = 15, scale = 2)
    private BigDecimal actualValue;

    @Column(name = "variance", precision = 15, scale = 2)
    private BigDecimal variance;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ServiceKpiValueStatus status = ServiceKpiValueStatus.FINAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    @Builder.Default
    private ServiceKpiValueSource source = ServiceKpiValueSource.SYSTEM;

    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;

    @Column(name = "recorded_by", columnDefinition = "uuid")
    private UUID recordedBy;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (this.recordedAt == null) {
            this.recordedAt = now;
        }
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}

