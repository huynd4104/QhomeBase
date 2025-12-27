package com.QhomeBase.datadocsservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "video_storage", schema = "files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoStorage {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "original_file_name", nullable = false, length = 500)
    private String originalFileName;

    @Column(name = "file_path", nullable = false, columnDefinition = "TEXT")
    private String filePath;

    @Column(name = "file_url", nullable = false, columnDefinition = "TEXT")
    private String fileUrl;

    @Column(name = "content_type", nullable = false, length = 100)
    @Builder.Default
    private String contentType = "video/mp4";

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "category", nullable = false, length = 50)
    private String category; // 'repair_request', 'marketplace_post', 'direct_chat', 'group_chat', 'marketplace_comment'

    @Column(name = "owner_id")
    private UUID ownerId; // ID của entity sở hữu video (post_id, conversation_id, group_id, request_id)

    @Column(name = "resolution", length = 20)
    private String resolution; // '480p', '720p', '1080p', etc.

    @Column(name = "duration_seconds")
    private Integer durationSeconds; // Thời lượng video (giây)

    @Column(name = "width")
    private Integer width; // Chiều rộng video (pixels)

    @Column(name = "height")
    private Integer height; // Chiều cao video (pixels)

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private OffsetDateTime uploadedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;
}
