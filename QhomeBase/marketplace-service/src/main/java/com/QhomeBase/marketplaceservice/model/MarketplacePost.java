package com.QhomeBase.marketplaceservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "marketplace_posts", schema = "marketplace", indexes = {
    @Index(name = "idx_posts_building", columnList = "building_id"),
    @Index(name = "idx_posts_category", columnList = "category"),
    @Index(name = "idx_posts_status", columnList = "status"),
    @Index(name = "idx_posts_created", columnList = "created_at"),
    @Index(name = "idx_posts_resident", columnList = "resident_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketplacePost {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "resident_id", nullable = false)
    private UUID residentId;

    @Column(name = "building_id", nullable = false)
    private UUID buildingId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "scope", nullable = false, columnDefinition = "post_scope")
    @Builder.Default
    private PostScope scope = PostScope.BUILDING;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "price", precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "post_status")
    @Builder.Default
    private PostStatus status = PostStatus.ACTIVE;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "contact_info", columnDefinition = "JSONB")
    private String contactInfo; // JSON string: {"phone": "...", "email": "...", "showPhone": true, "showEmail": false}

    @Column(name = "location", length = 200)
    private String location; // Tòa nhà, tầng, căn hộ

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Long viewCount = 0L;

    @Column(name = "like_count", nullable = false)
    @Builder.Default
    private Long likeCount = 0L;

    @Column(name = "comment_count", nullable = false)
    @Builder.Default
    private Long commentCount = 0L;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 50)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<MarketplacePostImage> images = new ArrayList<>();

    @Column(name = "video_url", columnDefinition = "TEXT")
    private String videoUrl; // URL to video stored in data-docs-service VideoStorage

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 50)
    @Builder.Default
    private List<MarketplaceLike> likes = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 50)
    @Builder.Default
    private List<MarketplaceComment> comments = new ArrayList<>();

    public void addImage(MarketplacePostImage image) {
        images.add(image);
        image.setPost(this);
    }

    public void removeImage(MarketplacePostImage image) {
        images.remove(image);
        image.setPost(null);
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public void incrementLikeCount() {
        this.likeCount++;
    }

    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    public void incrementCommentCount() {
        this.commentCount++;
    }

    public void decrementCommentCount() {
        if (this.commentCount > 0) {
            this.commentCount--;
        }
    }

    public boolean isActive() {
        return status == PostStatus.ACTIVE;
    }
}

