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
public class WebSocketMessage {
    private String type; // NEW_MESSAGE, MESSAGE_UPDATED, MESSAGE_DELETED, MEMBER_ADDED, MEMBER_REMOVED, GROUP_UPDATED, DIRECT_MESSAGE, DIRECT_INVITATION, etc.
    private UUID groupId;
    private UUID conversationId; // For direct chat
    private MessageResponse message;
    private DirectMessageResponse directMessage; // For direct chat
    private GroupResponse group;
    private GroupMemberResponse member;
    private DirectInvitationResponse directInvitation; // For direct chat invitations
    private OffsetDateTime timestamp;
}

