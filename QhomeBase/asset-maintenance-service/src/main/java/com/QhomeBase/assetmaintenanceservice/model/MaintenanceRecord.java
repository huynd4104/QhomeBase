package com.QhomeBase.assetmaintenanceservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "maintenance_records", schema = "asset")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(name = "maintenance_type", nullable = false)
    private String maintenanceType; 

    @Column(name = "maintenance_date", nullable = false)
    private LocalDate maintenanceDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maintenance_schedule_id")
    private MaintenanceSchedule maintenanceSchedule;

    @Column(name = "assigned_to", nullable = false)
    private UUID assignedTo;

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "completion_report", columnDefinition = "TEXT")
    private String completionReport;

    @Column(name = "technician_report", columnDefinition = "TEXT")
    private String technicianReport;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "cost", precision = 15, scale = 2)
    private BigDecimal cost;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "parts_replaced", columnDefinition = "text[]")
    @Builder.Default
    private List<String> partsReplaced = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "completion_images", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> completionImages = new ArrayList<>();

    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "SCHEDULED"; // 'SCHEDULED', 'ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'FAILED'

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @Column(name = "created_by", nullable = false)
    @Builder.Default
    private String createdBy = "system";

    @Column(name = "updated_by")
    private String updatedBy;

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = Instant.now();
    }
}

