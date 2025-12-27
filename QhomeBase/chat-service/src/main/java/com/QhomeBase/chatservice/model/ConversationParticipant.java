package com.QhomeBase.chatservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "conversation_participants", schema = "chat_service", indexes = {
    @Index(name = "idx_conversation_participants_conversation_id", columnList = "conversation_id"),
    @Index(name = "idx_conversation_participants_resident_id", columnList = "resident_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Column(name = "conversation_id", insertable = false, updatable = false)
    private UUID conversationId;

    @Column(name = "resident_id", nullable = false)
    private UUID residentId;

    @Column(name = "last_read_at")
    private OffsetDateTime lastReadAt; // Last time this participant read messages

    @Column(name = "is_muted", nullable = false)
    @Builder.Default
    private Boolean isMuted = false;

    @Column(name = "is_blocked", nullable = false)
    @Builder.Default
    private Boolean isBlocked = false; // If this participant blocked the other

    @Column(name = "mute_until")
    private OffsetDateTime muteUntil; // Timestamp when mute expires (null = not muted or muted indefinitely)

    @Column(name = "muted_by_user_id")
    private UUID mutedByUserId; // User who set the mute

    @Column(name = "is_hidden", nullable = false)
    @Builder.Default
    private Boolean isHidden = false; // If conversation is hidden from user's chat list

    @Column(name = "hidden_at")
    private OffsetDateTime hiddenAt; // When conversation was hidden

    @CreationTimestamp
    @Column(name = "joined_at", nullable = false, updatable = false)
    private OffsetDateTime joinedAt;
}

