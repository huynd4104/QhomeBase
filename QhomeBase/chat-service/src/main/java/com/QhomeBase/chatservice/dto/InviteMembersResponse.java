package com.QhomeBase.chatservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteMembersResponse {
    private List<GroupInvitationResponse> successfulInvitations;
    private List<String> invalidPhones; // Phone numbers that don't exist in database
    private List<String> skippedPhones; // Phone numbers that were skipped (already member, already invited, etc.)
}

