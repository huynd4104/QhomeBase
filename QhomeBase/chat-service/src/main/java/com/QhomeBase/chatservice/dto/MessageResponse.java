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
public class MessageResponse {
    private UUID id;
    private UUID groupId;
    private UUID senderId;
    private String senderName;
    private String senderAvatar;
    private String content;
    private String messageType; // TEXT, IMAGE, FILE, SYSTEM
    private String imageUrl;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private String mimeType;
    private UUID replyToMessageId;
    private MessageResponse replyToMessage; // Full reply message if exists
    private Boolean isEdited;
    private Boolean isDeleted;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    
    // Marketplace post fields (for MARKETPLACE_POST message type)
    private String postId;
    private String postTitle;
    private String postThumbnailUrl;
    private Double postPrice;
    private String deepLink;
    private String postStatus; // ACTIVE, SOLD, DELETED - checked from marketplace service
}

