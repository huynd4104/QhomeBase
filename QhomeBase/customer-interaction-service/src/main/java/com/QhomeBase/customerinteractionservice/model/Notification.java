package com.QhomeBase.customerinteractionservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications", schema = "content")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "type", nullable = false, columnDefinition = "notification_type")
    private NotificationType type;

    @Column(name = "title", nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "scope", nullable = false, columnDefinition = "notification_scope")
    private NotificationScope scope;

    @Column(name = "target_role", length = 50)
    private String targetRole;

    @Column(name = "target_building_id")
    private UUID targetBuildingId;

    @Column(name = "target_resident_id")
    private UUID targetResidentId;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "action_url", columnDefinition = "TEXT")
    private String actionUrl;

    @Column(name = "icon_url", columnDefinition = "TEXT")
    private String iconUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by")
    private UUID deletedBy;

    public boolean isDeleted() {
        return deletedAt != null;
    }

    @PrePersist
    private void validate() {
        if (scope == NotificationScope.INTERNAL) {
            if (targetBuildingId != null) {
                throw new IllegalStateException("INTERNAL notification cannot have target_building_id");
            }
            if (targetRole == null) {
                throw new IllegalStateException("INTERNAL notification must have target_role (use 'ALL' for all roles)");
            }
        } else if (scope == NotificationScope.EXTERNAL) {
            if (targetRole != null) {
                throw new IllegalStateException("EXTERNAL notification cannot have target_role");
            }
        }
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = Instant.now();
        validate();
    }
}
