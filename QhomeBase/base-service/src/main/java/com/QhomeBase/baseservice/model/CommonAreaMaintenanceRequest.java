package com.QhomeBase.baseservice.model;

import com.QhomeBase.baseservice.util.StringListConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "common_area_maintenance_requests", schema = "data")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommonAreaMaintenanceRequest {

    @Id
    private UUID id;

    @Column(name = "building_id")
    private UUID buildingId; // Optional - for building-specific requests

    @Column(name = "resident_id")
    private UUID residentId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "area_type", nullable = false)
    private String areaType; // e.g., "Hành lang", "Thang máy", "Đèn khu vực chung", "Bãi xe", etc.

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Convert(converter = StringListConverter.class)
    @Column(name = "attachments", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> attachments = new ArrayList<>();

    @Column(name = "location", nullable = false)
    private String location; // Specific location description

    @Column(name = "contact_name", nullable = false)
    private String contactName;

    @Column(name = "contact_phone", nullable = false)
    private String contactPhone;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "note")
    private String note;

    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "admin_response", columnDefinition = "TEXT")
    private String adminResponse; // Optional - admin can add notes when approve/deny/complete

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
