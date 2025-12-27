package com.QhomeBase.chatservice.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateMessageRequest {
    
    @Size(max = 5000, message = "Message content must not exceed 5000 characters")
    private String content;
    
    private String messageType; // TEXT, IMAGE, AUDIO, FILE, SYSTEM
    
    private String imageUrl;
    
    private String fileUrl; // For both audio and file messages
    
    private String fileName;
    
    private Long fileSize;
    
    private String mimeType;
    
    private UUID replyToMessageId; // For replying to a message
    
    // Marketplace post fields (for MARKETPLACE_POST message type)
    private String postId;
    private String postTitle;
    private String postThumbnailUrl;
    private Double postPrice;
    private String deepLink;
}

