package com.QhomeBase.marketplaceservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "marketplace_likes", schema = "marketplace", 
    uniqueConstraints = @UniqueConstraint(name = "uq_likes_post_resident", columnNames = {"post_id", "resident_id"}),
    indexes = {
        @Index(name = "idx_likes_post", columnList = "post_id"),
        @Index(name = "idx_likes_resident", columnList = "resident_id")
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketplaceLike {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false, foreignKey = @ForeignKey(name = "fk_likes_post"))
    private MarketplacePost post;

    @Column(name = "resident_id", nullable = false)
    private UUID residentId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}

