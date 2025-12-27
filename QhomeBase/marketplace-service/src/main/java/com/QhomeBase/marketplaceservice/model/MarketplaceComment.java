package com.QhomeBase.marketplaceservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "marketplace_comments", schema = "marketplace", indexes = {
    @Index(name = "idx_comments_post", columnList = "post_id"),
    @Index(name = "idx_comments_resident", columnList = "resident_id"),
    @Index(name = "idx_comments_parent", columnList = "parent_comment_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketplaceComment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false, foreignKey = @ForeignKey(name = "fk_comments_post"))
    private MarketplacePost post;

    @Column(name = "resident_id", nullable = false)
    private UUID residentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id", foreignKey = @ForeignKey(name = "fk_comments_parent"))
    private MarketplaceComment parentComment;

    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<MarketplaceComment> replies = new ArrayList<>();

    @Column(name = "content", nullable = true, columnDefinition = "TEXT")
    private String content; // Can be null if comment only has image or video

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl; // URL of image attached to comment

    @Column(name = "video_url", columnDefinition = "TEXT")
    private String videoUrl; // URL of video attached to comment

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void markAsDeleted() {
        this.deletedAt = OffsetDateTime.now();
    }
}

