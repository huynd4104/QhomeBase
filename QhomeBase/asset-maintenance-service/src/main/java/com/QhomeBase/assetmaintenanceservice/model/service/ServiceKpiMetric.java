package com.QhomeBase.assetmaintenanceservice.model.service;

import com.QhomeBase.assetmaintenanceservice.model.service.enums.ServiceKpiFrequency;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "service_kpi_metric", schema = "asset",
       uniqueConstraints = @UniqueConstraint(columnNames = {"service_id", "code"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceKpiMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private Service service;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "unit")
    private String unit;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false)
    @Builder.Default
    private ServiceKpiFrequency frequency = ServiceKpiFrequency.MONTHLY;

    @Column(name = "calculation_method", columnDefinition = "TEXT")
    private String calculationMethod;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "metric", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ServiceKpiTarget> targets = new ArrayList<>();

    @OneToMany(mappedBy = "metric", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ServiceKpiValue> values = new ArrayList<>();

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

