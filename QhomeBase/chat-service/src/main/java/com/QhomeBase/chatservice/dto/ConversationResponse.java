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
public class ConversationResponse {
    private UUID id;
    private UUID participant1Id;
    private UUID participant2Id;
    private String participant1Name;
    private String participant2Name;
    private String status; // PENDING, ACTIVE, BLOCKED, CLOSED
    private UUID createdBy;
    private DirectMessageResponse lastMessage; // Last message in conversation
    private Long unreadCount; // Unread message count for current user
    private OffsetDateTime lastReadAt; // Last read timestamp for current user
    private Boolean isBlockedByOther; // True if current user is blocked by the other participant
    private Boolean isBlockedByMe; // True if current user has blocked the other participant
    private Boolean areFriends; // True if current user and other participant are friends (active friendship)
    private Boolean canSendMessage; // True if current user can send messages (status=ACTIVE, areFriends=true, not blocked)
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}

