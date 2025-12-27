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
public class CreateDirectInvitationRequest {
    private UUID inviteeId; // Resident ID to invite (optional if phoneNumber is provided)
    private String phoneNumber; // Phone number to invite (optional if inviteeId is provided)
    private String initialMessage; // Optional first message
}

