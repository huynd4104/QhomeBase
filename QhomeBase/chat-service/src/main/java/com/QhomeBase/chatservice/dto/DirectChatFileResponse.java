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
public class DirectChatFileResponse {
    private UUID id;
    private UUID conversationId;
    private UUID messageId;
    private UUID senderId;
    private String senderName;
    private String fileName;
    private Long fileSize;
    private String fileType; // IMAGE, AUDIO, VIDEO, DOCUMENT
    private String mimeType;
    private String fileUrl;
    private OffsetDateTime createdAt;
}

