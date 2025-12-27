package com.QhomeBase.baseservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "services", schema = "data", uniqueConstraints = {
    @UniqueConstraint(name = "uq_services_code", columnNames = "code")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Service {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "name_en")
    private String nameEn;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    @Builder.Default
    private ServiceType type = ServiceType.UTILITY;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit", nullable = false)
    private ServiceUnit unit;

    @Column(name = "unit_label")
    private String unitLabel;

    @Column(name = "billable", nullable = false)
    @Builder.Default
    private Boolean billable = true;

    @Column(name = "requires_meter", nullable = false)
    @Builder.Default
    private Boolean requiresMeter = false;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "description")
    private String description;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}

