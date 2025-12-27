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
@Table(name = "group_files", schema = "chat_service", indexes = {
    @Index(name = "idx_group_files_group_id", columnList = "group_id"),
    @Index(name = "idx_group_files_created_at", columnList = "created_at"),
    @Index(name = "idx_group_files_sender_id", columnList = "sender_id"),
    @Index(name = "idx_group_files_message_id", columnList = "message_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupFile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @Column(name = "group_id", insertable = false, updatable = false)
    private UUID groupId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @Column(name = "message_id", insertable = false, updatable = false)
    private UUID messageId;

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "file_type", length = 50)
    private String fileType; // IMAGE, AUDIO, VIDEO, DOCUMENT

    @Column(name = "mime_type", length = 100)
    private String mimeType; // Actual mime type (e.g., image/jpeg, image/png, application/pdf)

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}

