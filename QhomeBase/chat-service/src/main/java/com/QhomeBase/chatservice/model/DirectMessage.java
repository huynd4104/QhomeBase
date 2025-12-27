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
@Table(name = "direct_messages", schema = "chat_service", indexes = {
    @Index(name = "idx_direct_messages_conversation_id", columnList = "conversation_id"),
    @Index(name = "idx_direct_messages_sender_id", columnList = "sender_id"),
    @Index(name = "idx_direct_messages_created_at", columnList = "created_at"),
    @Index(name = "idx_direct_messages_reply_to", columnList = "reply_to_message_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Column(name = "conversation_id", insertable = false, updatable = false)
    private UUID conversationId;

    @Column(name = "sender_id", nullable = true)
    private UUID senderId; // Null for system messages

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "message_type", nullable = false, length = 50)
    @Builder.Default
    private String messageType = "TEXT"; // TEXT, IMAGE, AUDIO, FILE, SYSTEM

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_message_id")
    private DirectMessage replyToMessage;

    @Column(name = "reply_to_message_id", insertable = false, updatable = false)
    private UUID replyToMessageId;

    @Column(name = "is_edited", nullable = false)
    @Builder.Default
    private Boolean isEdited = false;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}

