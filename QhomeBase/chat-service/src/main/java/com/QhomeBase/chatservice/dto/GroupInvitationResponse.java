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
public class GroupInvitationResponse {
    private UUID id;
    private UUID groupId;
    private String groupName;
    private UUID inviterId;
    private String inviterName;
    private String inviteePhone;
    private UUID inviteeResidentId;
    private String status; // PENDING, ACCEPTED, DECLINED (no longer EXPIRED - invitations don't expire)
    private OffsetDateTime createdAt;
    private OffsetDateTime expiresAt; // No longer used - invitations don't expire, only accept/decline changes status
}

