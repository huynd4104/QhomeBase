package com.QhomeBase.chatservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDirectMessageRequest {
    private String content;
    private String messageType; // TEXT, IMAGE, AUDIO, FILE
    private String imageUrl;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private String mimeType;
    private UUID replyToMessageId;
    
    // Marketplace post fields (for MARKETPLACE_POST message type)
    private String postId;
    private String postTitle;
    private String postThumbnailUrl;
    private Double postPrice;
    private String deepLink;
}

