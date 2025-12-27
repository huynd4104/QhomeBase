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
@Table(name = "conversations", schema = "chat_service", indexes = {
    @Index(name = "idx_conversations_participant1", columnList = "participant1_id"),
    @Index(name = "idx_conversations_participant2", columnList = "participant2_id"),
    @Index(name = "idx_conversations_status", columnList = "status"),
    @Index(name = "idx_conversations_updated_at", columnList = "updated_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "participant1_id", nullable = false)
    private UUID participant1Id; // Always the smaller UUID for uniqueness

    @Column(name = "participant2_id", nullable = false)
    private UUID participant2Id; // Always the larger UUID for uniqueness

    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private String status = "PENDING"; // PENDING, ACTIVE, BLOCKED, LOCKED, DELETED

    @Column(name = "created_by", nullable = false)
    private UUID createdBy; // Who initiated the conversation

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * Helper method to get the other participant's ID
     */
    public UUID getOtherParticipantId(UUID currentUserId) {
        if (participant1Id.equals(currentUserId)) {
            return participant2Id;
        } else if (participant2Id.equals(currentUserId)) {
            return participant1Id;
        }
        throw new IllegalArgumentException("User is not a participant in this conversation");
    }

    /**
     * Check if a user is a participant in this conversation
     */
    public boolean isParticipant(UUID userId) {
        return participant1Id.equals(userId) || participant2Id.equals(userId);
    }
}

