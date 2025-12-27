package com.QhomeBase.chatservice.model;

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
@Table(name = "friendships", schema = "chat_service", indexes = {
    @Index(name = "idx_friendships_user1", columnList = "user1_id"),
    @Index(name = "idx_friendships_user2", columnList = "user2_id"),
    @Index(name = "idx_friendships_is_active", columnList = "is_active")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user1_id", nullable = false)
    private UUID user1Id; // Always the smaller UUID for uniqueness

    @Column(name = "user2_id", nullable = false)
    private UUID user2Id; // Always the larger UUID for uniqueness

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true; // FALSE when one user blocks the other

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * Check if a user is part of this friendship
     */
    public boolean involvesUser(UUID userId) {
        return user1Id.equals(userId) || user2Id.equals(userId);
    }

    /**
     * Get the other user's ID
     */
    public UUID getOtherUserId(UUID currentUserId) {
        if (user1Id.equals(currentUserId)) {
            return user2Id;
        } else if (user2Id.equals(currentUserId)) {
            return user1Id;
        }
        throw new IllegalArgumentException("User is not part of this friendship");
    }
}

