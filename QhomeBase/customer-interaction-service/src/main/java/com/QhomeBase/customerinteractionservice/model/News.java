package com.QhomeBase.customerinteractionservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Entity
@Table(name = "news", schema = "content")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class News {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "title", nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "body_html", nullable = false, columnDefinition = "TEXT")
    private String bodyHtml;

    @Column(name = "cover_image_url", columnDefinition = "TEXT")
    private String coverImageUrl;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "news_status")
    @Builder.Default
    private NewsStatus status = NewsStatus.DRAFT;

    @Column(name = "publish_at")
    private Instant publishAt;

    @Column(name = "expire_at")
    private Instant expireAt;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Long viewCount = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "scope", columnDefinition = "notification_scope")
    private NotificationScope scope;

    @Column(name = "target_role", length = 50)
    private String targetRole;

    @Column(name = "target_building_id")
    private UUID targetBuildingId;

    @OneToMany(mappedBy = "news", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<NewsImage> images = new ArrayList<>();


    @PrePersist
    private void validate() {
        if (scope != null) {
            if (scope == NotificationScope.INTERNAL) {
                if (targetBuildingId != null) {
                    throw new IllegalStateException("INTERNAL news cannot have target_building_id");
                }
                if (targetRole == null) {
                    throw new IllegalStateException("INTERNAL news must have target_role (use 'ALL' for all roles)");
                }
            } else if (scope == NotificationScope.EXTERNAL) {
                if (targetRole != null) {
                    throw new IllegalStateException("EXTERNAL news cannot have target_role");
                }
            }
        }
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = Instant.now();
        validate();
    }


    public void addImage(NewsImage image) {
        images.add(image);
        image.setNews(this);
    }

    public void removeImage(NewsImage image) {
        images.remove(image);
        image.setNews(null);
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public boolean isActive() {
        if (status != NewsStatus.PUBLISHED && status != NewsStatus.SCHEDULED) {
            return false;
        }

        Instant now = Instant.now();

        if (publishAt != null && publishAt.isAfter(now)) {
            return false;
        }

        if (expireAt != null && expireAt.isBefore(now)) {
            return false;
        }

        return true;
    }
}


