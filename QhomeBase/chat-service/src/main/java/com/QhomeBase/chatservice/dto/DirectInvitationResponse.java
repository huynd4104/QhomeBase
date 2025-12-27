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
public class DirectInvitationResponse {
    private UUID id;
    private UUID conversationId;
    private UUID inviterId;
    private String inviterName;
    private UUID inviteeId;
    private String inviteeName;
    private String status; // PENDING, ACCEPTED, DECLINED, EXPIRED
    private String initialMessage;
    private OffsetDateTime createdAt;
    private OffsetDateTime expiresAt;
    private OffsetDateTime respondedAt;
}

