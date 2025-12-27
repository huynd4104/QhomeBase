package com.QhomeBase.baseservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "buildings", schema = "data", uniqueConstraints = @UniqueConstraint(name = "uq_buildings_code", columnNames = {"code"}))
@Data
@NoArgsConstructor @AllArgsConstructor @Builder
public class Building {
    @GeneratedValue
    @Id
    private UUID id;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "address")
    private String address;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "created_by", nullable = false)
    @Builder.Default
    private String createdBy = "system";

    @Column(name = "updated_by")
    private String updatedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    private BuildingStatus status = BuildingStatus.ACTIVE;

    @Column(name = "number_of_floors")
    private Integer numberOfFloors;

    public UUID getId() {
        return this.id;
    }
}
