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
@Table(name = "direct_message_deletions", schema = "chat_service", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"message_id", "deleted_by_user_id", "delete_type"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectMessageDeletion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "message_id", nullable = false)
    private UUID messageId;

    @Column(name = "deleted_by_user_id", nullable = false)
    private UUID deletedByUserId;

    @Column(name = "delete_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private DeleteType deleteType;

    @CreationTimestamp
    @Column(name = "deleted_at", nullable = false, updatable = false)
    private OffsetDateTime deletedAt;

    public enum DeleteType {
        FOR_ME,        // Only deleted for the user who deleted it
        FOR_EVERYONE   // Deleted for everyone in the conversation
    }
}

