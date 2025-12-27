package com.QhomeBase.chatservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupResponse {
    private UUID id;
    private String name;
    private String description;
    private UUID createdBy;
    private String createdByName;
    private UUID buildingId;
    private String buildingName;
    private String avatarUrl;
    private Integer maxMembers;
    private Integer currentMemberCount;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<GroupMemberResponse> members;
    private String userRole; // Role of current user in this group
    private Long unreadCount; // Unread messages for current user
}

