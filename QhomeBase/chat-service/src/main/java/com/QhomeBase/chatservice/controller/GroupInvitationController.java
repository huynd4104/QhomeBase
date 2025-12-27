package com.QhomeBase.chatservice.controller;

import com.QhomeBase.chatservice.dto.GroupInvitationResponse;
import com.QhomeBase.chatservice.dto.InviteMembersByPhoneRequest;
import com.QhomeBase.chatservice.dto.InviteMembersResponse;
import com.QhomeBase.chatservice.security.UserPrincipal;
import com.QhomeBase.chatservice.service.GroupInvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
@Tag(name = "Group Invitations", description = "Group invitation management APIs")
@Slf4j
public class GroupInvitationController {

    private final GroupInvitationService invitationService;

    @PostMapping("/{groupId}/invite")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Invite members by phone number", description = "Invite members to group using phone numbers. Validates that phone numbers exist in the system.")
    public ResponseEntity<?> inviteMembersByPhone(
            @PathVariable UUID groupId,
            @Valid @RequestBody InviteMembersByPhoneRequest request,
            Authentication authentication) {
        
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        log.info("📨 [GroupInvitationController] inviteMembersByPhone called - groupId: {}, userId: {}, phoneNumbers: {}", 
            groupId, userId, request.getPhoneNumbers());
        
        try {
            InviteMembersResponse response = invitationService.inviteMembersByPhone(groupId, request, userId);
            
            log.info("📨 [GroupInvitationController] inviteMembersByPhone completed - successful: {}, invalid: {}, skipped: {}", 
                response.getSuccessfulInvitations() != null ? response.getSuccessfulInvitations().size() : 0,
                response.getInvalidPhones() != null ? response.getInvalidPhones().size() : 0,
                response.getSkippedPhones() != null ? response.getSkippedPhones().size() : 0);
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.warn("Failed to invite members: {}", e.getMessage());
            // Return error response with clear message
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", true);
            errorResponse.put("message", e.getMessage() != null ? e.getMessage() : "Không thể mời thành viên. Vui lòng thử lại.");
            errorResponse.put("code", "INVITATION_ERROR");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/invitations/my")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Get my pending invitations", description = "Get list of pending group invitations for current user")
    public ResponseEntity<List<GroupInvitationResponse>> getMyPendingInvitations(
            Authentication authentication) {
        
        if (authentication == null) {
            log.error("❌ [GroupInvitationController] Authentication is null!");
            return ResponseEntity.status(403).build();
        }
        
        if (authentication.getPrincipal() == null) {
            log.error("❌ [GroupInvitationController] Principal is null!");
            return ResponseEntity.status(403).build();
        }
        
        log.info("📋 [GroupInvitationController] Authentication: {}", authentication.getClass().getSimpleName());
        log.info("📋 [GroupInvitationController] Principal: {}", authentication.getPrincipal().getClass().getSimpleName());
        log.info("📋 [GroupInvitationController] Authorities: {}", authentication.getAuthorities());
        
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        log.info("📋 [GroupInvitationController] getMyPendingInvitations called - userId: {}, roles: {}", userId, principal.roles());
        
        List<GroupInvitationResponse> response = invitationService.getMyPendingInvitations(userId);
        
        log.info("📋 [GroupInvitationController] getMyPendingInvitations returning {} invitations for userId: {}", 
            response.size(), userId);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{groupId}/invitations")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Get all invitations for a group", description = "Get list of all invitations (PENDING and ACCEPTED) for a specific group, including invitations sent by current user")
    public ResponseEntity<List<GroupInvitationResponse>> getGroupInvitations(
            @PathVariable UUID groupId,
            Authentication authentication) {
        
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        log.info("📋 [GroupInvitationController] getGroupInvitations called - groupId: {}, userId: {}", groupId, userId);
        
        List<GroupInvitationResponse> response = invitationService.getGroupInvitations(groupId, userId);
        
        log.info("📋 [GroupInvitationController] getGroupInvitations returning {} invitations for groupId: {}", 
            response.size(), groupId);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/invitations/{invitationId}/accept")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Accept invitation", description = "Accept a group invitation")
    public ResponseEntity<Void> acceptInvitation(
            @PathVariable UUID invitationId,
            Authentication authentication) {
        
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        invitationService.acceptInvitation(invitationId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/invitations/{invitationId}/decline")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Decline invitation", description = "Decline a group invitation")
    public ResponseEntity<Void> declineInvitation(
            @PathVariable UUID invitationId,
            Authentication authentication) {
        
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        invitationService.declineInvitation(invitationId, userId);
        return ResponseEntity.ok().build();
    }
}

