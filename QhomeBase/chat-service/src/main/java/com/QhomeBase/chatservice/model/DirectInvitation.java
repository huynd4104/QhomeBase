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
@Table(name = "direct_invitations", schema = "chat_service", indexes = {
    @Index(name = "idx_direct_invitations_conversation_id", columnList = "conversation_id"),
    @Index(name = "idx_direct_invitations_inviter_id", columnList = "inviter_id"),
    @Index(name = "idx_direct_invitations_invitee_id", columnList = "invitee_id"),
    @Index(name = "idx_direct_invitations_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Column(name = "conversation_id", insertable = false, updatable = false)
    private UUID conversationId;

    @Column(name = "inviter_id", nullable = false)
    private UUID inviterId; // Resident who sent the invitation

    @Column(name = "invitee_id", nullable = false)
    private UUID inviteeId; // Resident who receives the invitation

    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private String status = "PENDING"; // PENDING, ACCEPTED, DECLINED (no longer EXPIRED - invitations don't expire)

    @Column(name = "initial_message", columnDefinition = "TEXT")
    private String initialMessage; // First message sent with invitation (optional)

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt; // No longer used - invitations don't expire, only accept/decline changes status

    @Column(name = "responded_at")
    private OffsetDateTime respondedAt;
}

