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
public class FriendResponse {
    private UUID friendId;
    private String friendName;
    private String friendPhone;
    private UUID conversationId; // If conversation exists
    private Boolean hasActiveConversation; // True if conversation exists and is ACTIVE
}

