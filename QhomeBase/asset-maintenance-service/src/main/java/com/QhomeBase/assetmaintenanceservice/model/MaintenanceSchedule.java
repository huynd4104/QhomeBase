package com.QhomeBase.assetmaintenanceservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "maintenance_schedules", schema = "asset")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(name = "maintenance_type", nullable = false)
    private String maintenanceType; // 'ROUTINE', 'REPAIR', 'INSPECTION', 'EMERGENCY', 'UPGRADE'

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "interval_days", nullable = false)
    private Integer intervalDays;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "next_maintenance_date", nullable = false)
    private LocalDate nextMaintenanceDate;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

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

    @OneToMany(mappedBy = "maintenanceSchedule", cascade = CascadeType.ALL)
    @Builder.Default
    private List<MaintenanceRecord> maintenanceRecords = new ArrayList<>();

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public void calculateNextMaintenanceDate() {
        if (startDate != null && intervalDays != null) {
            this.nextMaintenanceDate = startDate.plusDays(intervalDays);
        }
    }
}

