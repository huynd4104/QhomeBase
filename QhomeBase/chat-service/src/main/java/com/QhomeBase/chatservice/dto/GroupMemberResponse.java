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
public class GroupMemberResponse {
    private UUID id;
    private UUID groupId;
    private UUID residentId;
    private String residentName;
    private String residentAvatar;
    private String role; // ADMIN, MODERATOR, MEMBER
    private OffsetDateTime joinedAt;
    private OffsetDateTime lastReadAt;
    private Boolean isMuted;
}

