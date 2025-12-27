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
public class DirectMessageResponse {
    private UUID id;
    private UUID conversationId;
    private UUID senderId;
    private String senderName;
    private String content;
    private String messageType; // TEXT, IMAGE, AUDIO, FILE, SYSTEM
    private String imageUrl;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private String mimeType;
    private UUID replyToMessageId;
    private DirectMessageResponse replyToMessage; // Nested reply message
    private Boolean isEdited;
    private Boolean isDeleted;
    private String deleteType; // FOR_ME, FOR_EVERYONE, or null if not deleted
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

