package com.QhomeBase.chatservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupFileResponse {
    private UUID id;
    private UUID groupId;
    private UUID messageId;
    private UUID senderId;
    private String senderName;
    private String senderAvatar;
    private String fileName;
    private Long fileSize;
    private String fileType; // IMAGE, AUDIO, VIDEO, DOCUMENT (legacy)
    private String mimeType; // Actual mime type (e.g., image/jpeg, image/png, application/pdf)
    private String fileUrl;
    private OffsetDateTime createdAt;
}

