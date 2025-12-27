package com.QhomeBase.chatservice.controller;

import com.QhomeBase.chatservice.dto.*;
import com.QhomeBase.chatservice.model.Block;
import com.QhomeBase.chatservice.repository.BlockRepository;
import com.QhomeBase.chatservice.security.UserPrincipal;
import com.QhomeBase.chatservice.service.BlockService;
import com.QhomeBase.chatservice.service.DirectChatService;
import com.QhomeBase.chatservice.service.ResidentInfoService;
import com.QhomeBase.chatservice.service.ConversationMuteService;
import com.QhomeBase.chatservice.service.ConversationHideService;
import com.QhomeBase.chatservice.dto.FriendResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/direct-chat")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Direct Chat", description = "APIs for 1-1 direct chat")
public class DirectChatController {

    private final DirectChatService directChatService;
    private final BlockService blockService;
    private final BlockRepository blockRepository;
    private final ResidentInfoService residentInfoService;
    private final ConversationMuteService conversationMuteService;
    private final ConversationHideService conversationHideService;

    @GetMapping("/conversations")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Get all conversations", description = "Get all active conversations for current user")
    public ResponseEntity<List<ConversationResponse>> getConversations(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        log.info("📋 [DirectChatController] getConversations called - userId: {}", userId);
        List<ConversationResponse> conversations = directChatService.getConversations(userId);
        log.info("📋 [DirectChatController] getConversations returning {} conversations for userId: {}", conversations.size(), userId);
        return ResponseEntity.ok(conversations);
    }

    @GetMapping("/conversations/{conversationId}")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Get conversation", description = "Get conversation details by ID")
    public ResponseEntity<ConversationResponse> getConversation(
            @PathVariable UUID conversationId,
            Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        ConversationResponse conversation = directChatService.getConversation(conversationId, userId);
        return ResponseEntity.ok(conversation);
    }

    @PostMapping("/conversations/{conversationId}/messages")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Send message", description = "Send a message in a direct chat conversation")
    public ResponseEntity<DirectMessageResponse> sendMessage(
            @PathVariable UUID conversationId,
            @RequestBody CreateDirectMessageRequest request,
            Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        DirectMessageResponse message = directChatService.createMessage(conversationId, request, userId);
        return ResponseEntity.ok(message);
    }

    @GetMapping("/conversations/{conversationId}/messages")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Get messages", description = "Get messages in a conversation with pagination")
    public ResponseEntity<DirectMessagePagedResponse> getMessages(
            @PathVariable UUID conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        DirectMessagePagedResponse messages = directChatService.getMessages(conversationId, userId, page, size);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/conversations/{conversationId}/unread-count")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Get unread count", description = "Get unread message count in a conversation")
    public ResponseEntity<Long> getUnreadCount(
            @PathVariable UUID conversationId,
            Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        Long unreadCount = directChatService.countUnreadMessages(conversationId, userId);
        return ResponseEntity.ok(unreadCount);
    }

    @GetMapping("/friends")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Get friends list", description = "Get all active friends for current user")
    public ResponseEntity<List<FriendResponse>> getFriends(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        List<FriendResponse> friends = directChatService.getFriends(userId);
        return ResponseEntity.ok(friends);
    }

    @GetMapping("/conversations/{conversationId}/files")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Get files", description = "Get files in a conversation with pagination")
    public ResponseEntity<DirectChatFilePagedResponse> getFiles(
            @PathVariable UUID conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        DirectChatFilePagedResponse files = directChatService.getFiles(conversationId, userId, page, size);
        return ResponseEntity.ok(files);
    }

    @PostMapping("/block/{blockedId}")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Block user", description = "Block a user from sending/receiving messages")
    public ResponseEntity<Void> blockUser(
            @PathVariable UUID blockedId,
            Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        // Convert userId to residentId
        UUID blockerResidentId = residentInfoService.getResidentIdFromUserId(userId);
        if (blockerResidentId == null) {
            log.error("Cannot find residentId for blocker userId: {}", userId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        
        // Try to convert blockedId (might be userId or residentId)
        UUID blockedResidentId = residentInfoService.getResidentIdFromUserId(blockedId);
        // If conversion returns null, assume blockedId is already a residentId
        if (blockedResidentId == null) {
            blockedResidentId = blockedId;
            log.debug("blockedId {} is already a residentId, using it directly", blockedId);
        }
        
        if (blockedResidentId == null) {
            log.error("Cannot determine residentId for blocked user: {}", blockedId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        
        blockService.blockUser(blockerResidentId, blockedResidentId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/block/{blockedId}")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Unblock user", description = "Unblock a user")
    public ResponseEntity<Void> unblockUser(
            @PathVariable UUID blockedId,
            Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        // Convert userId to residentId
        UUID blockerResidentId = residentInfoService.getResidentIdFromUserId(userId);
        if (blockerResidentId == null) {
            log.error("Cannot find residentId for blocker userId: {}", userId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        
        // Try to convert blockedId (might be userId or residentId)
        UUID blockedResidentId = residentInfoService.getResidentIdFromUserId(blockedId);
        // If conversion returns null, assume blockedId is already a residentId
        if (blockedResidentId == null) {
            blockedResidentId = blockedId;
            log.debug("blockedId {} is already a residentId, using it directly", blockedId);
        }
        
        if (blockedResidentId == null) {
            log.error("Cannot determine residentId for blocked user: {}", blockedId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        
        try {
            blockService.unblockUser(blockerResidentId, blockedResidentId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            log.error("Error unblocking user {}: {}", blockedResidentId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/blocked-users")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Get blocked users", description = "Get list of users blocked by current user")
    public ResponseEntity<List<UUID>> getBlockedUsers(Authentication authentication) {
        long startTime = System.currentTimeMillis();
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            UUID userId = principal.uid();
            
            // Convert userId to residentId
            UUID residentId = residentInfoService.getResidentIdFromUserId(userId);
            if (residentId == null) {
                // If residentId is null, return empty list instead of throwing error
                // This can happen if user doesn't have a resident record yet
                return ResponseEntity.ok(List.of());
            }
            
            // Get blocked residentIds - return residentIds directly (not userIds)
            // This is needed for marketplace filtering which uses residentIds
            List<UUID> blockedResidentIds = blockService.getBlockedUserIds(residentId);
            
            long duration = System.currentTimeMillis() - startTime;
            if (duration > 500) {
                log.warn("getBlockedUsers took {}ms (target: <500ms) for residentId: {}", duration, residentId);
            }
            
            return ResponseEntity.ok(blockedResidentIds);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.warn("Error getting blocked users after {}ms: {}", duration, e.getMessage());
            // Return empty list as fallback instead of error
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/blocked-by-users")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Get users who blocked current user", description = "Get list of users who have blocked current user")
    public ResponseEntity<List<UUID>> getBlockedByUsers(Authentication authentication) {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            UUID userId = principal.uid();
            
            // Convert userId to residentId
            UUID residentId = residentInfoService.getResidentIdFromUserId(userId);
            if (residentId == null) {
                // If residentId is null, return empty list instead of throwing error
                return ResponseEntity.ok(List.of());
            }
            
            // Get users who blocked current user (blockedId = current user)
            List<UUID> blockerResidentIds = blockRepository.findByBlockedId(residentId).stream()
                    .map(Block::getBlockerId)
                    .toList();
            
            log.info("Found {} users who blocked current user {}", blockerResidentIds.size(), residentId);
            return ResponseEntity.ok(blockerResidentIds);
            
        } catch (Exception e) {
            log.error("Error getting blocked-by users", e);
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/is-blocked/{userId}")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Check if user is blocked", description = "Check if a user is blocked by current user")
    public ResponseEntity<Boolean> isBlocked(
            @PathVariable UUID userId,
            Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID blockerUserId = principal.uid();
        
        // Convert userIds to residentIds
        UUID blockerResidentId = residentInfoService.getResidentIdFromUserId(blockerUserId);
        UUID blockedResidentId = residentInfoService.getResidentIdFromUserId(userId);
        
        boolean isBlocked = blockService.isBlocked(blockerResidentId, blockedResidentId);
        return ResponseEntity.ok(isBlocked);
    }

    @PostMapping("/conversations/{conversationId}/mute")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Mute direct conversation", description = "Mute notifications for a direct conversation. durationHours: 1, 2, 24, or null (indefinitely)")
    public ResponseEntity<Map<String, Object>> muteDirectConversation(
            @PathVariable UUID conversationId,
            @RequestParam(required = false) Integer durationHours,
            Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        boolean success = conversationMuteService.muteDirectConversation(conversationId, userId, durationHours);
        return ResponseEntity.ok(Map.of("success", success));
    }

    @DeleteMapping("/conversations/{conversationId}/mute")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Unmute direct conversation", description = "Unmute notifications for a direct conversation")
    public ResponseEntity<Map<String, Object>> unmuteDirectConversation(
            @PathVariable UUID conversationId,
            Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        boolean success = conversationMuteService.unmuteDirectConversation(conversationId, userId);
        return ResponseEntity.ok(Map.of("success", success));
    }

    @PostMapping("/conversations/{conversationId}/hide")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Hide direct conversation", description = "Hide a direct conversation from chat list (client-side only). Resets unreadCount to 0.")
    public ResponseEntity<Map<String, Object>> hideDirectConversation(
            @PathVariable UUID conversationId,
            Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        boolean success = conversationHideService.hideDirectConversation(conversationId, userId);
        return ResponseEntity.ok(Map.of("success", success));
    }

    @PutMapping("/conversations/{conversationId}/messages/{messageId}")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Edit direct message", description = "Edit a text message. Only the sender can edit their own messages.")
    public ResponseEntity<DirectMessageResponse> editMessage(
            @PathVariable UUID conversationId,
            @PathVariable UUID messageId,
            @RequestBody String content,
            Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        DirectMessageResponse response = directChatService.updateMessage(conversationId, messageId, content, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/conversations/{conversationId}/messages/{messageId}")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Delete direct message", description = "Delete a direct message. Only the sender can delete their own message. deleteType: FOR_ME (only for current user) or FOR_EVERYONE (for everyone)")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable UUID conversationId,
            @PathVariable UUID messageId,
            @RequestParam(defaultValue = "FOR_ME") String deleteType,
            Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        directChatService.deleteMessage(conversationId, messageId, userId, deleteType);
        return ResponseEntity.ok().build();
    }
}

