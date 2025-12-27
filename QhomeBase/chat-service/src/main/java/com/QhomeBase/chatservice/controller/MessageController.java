package com.QhomeBase.chatservice.controller;

import com.QhomeBase.chatservice.dto.CreateMessageRequest;
import com.QhomeBase.chatservice.dto.MessagePagedResponse;
import com.QhomeBase.chatservice.dto.MessageResponse;
import com.QhomeBase.chatservice.security.UserPrincipal;
import com.QhomeBase.chatservice.service.MessageService;
import com.QhomeBase.chatservice.service.ConversationMuteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/groups/{groupId}/messages")
@RequiredArgsConstructor
@Tag(name = "Messages", description = "Chat message APIs")
public class MessageController {

    private final MessageService messageService;
    private final ConversationMuteService conversationMuteService;

    @PostMapping
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Send a message", description = "Send a new message to the group")
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable UUID groupId,
            @Valid @RequestBody CreateMessageRequest request,
            Authentication authentication) {
        
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        MessageResponse response = messageService.createMessage(groupId, request, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Get messages", description = "Get paginated list of messages in the group")
    public ResponseEntity<MessagePagedResponse> getMessages(
            @PathVariable UUID groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication) {
        
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        MessagePagedResponse response = messageService.getMessages(groupId, userId, page, size);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{messageId}")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Edit message", description = "Edit a text message. Only the sender can edit their own messages.")
    public ResponseEntity<MessageResponse> editMessage(
            @PathVariable UUID groupId,
            @PathVariable UUID messageId,
            @RequestBody String content,
            Authentication authentication) {
        
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        MessageResponse response = messageService.updateMessage(groupId, messageId, content, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{messageId}")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Delete message", description = "Delete a message. Sender, admins, and moderators can delete messages.")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable UUID groupId,
            @PathVariable UUID messageId,
            Authentication authentication) {
        
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        messageService.deleteMessage(groupId, messageId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/mark-read")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Mark messages as read", description = "Mark all messages in the group as read for the current user")
    public ResponseEntity<Void> markMessagesAsRead(
            @PathVariable UUID groupId,
            Authentication authentication) {
        
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        messageService.markMessagesAsRead(groupId, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Get unread message count", description = "Get the number of unread messages in the group for the current user")
    public ResponseEntity<Map<String, Object>> getUnreadCount(
            @PathVariable UUID groupId,
            Authentication authentication) {
        
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        Long unreadCount = messageService.getUnreadCount(groupId, userId);
        return ResponseEntity.ok(Map.of("unreadCount", unreadCount));
    }

    @PostMapping("/mute")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Mute group chat", description = "Mute notifications for a group chat. durationHours: 1, 2, 24, or null (indefinitely)")
    public ResponseEntity<Map<String, Object>> muteGroupChat(
            @PathVariable UUID groupId,
            @RequestParam(required = false) Integer durationHours,
            Authentication authentication) {
        
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        boolean success = conversationMuteService.muteGroupChat(groupId, userId, durationHours);
        return ResponseEntity.ok(Map.of("success", success));
    }

    @DeleteMapping("/mute")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Unmute group chat", description = "Unmute notifications for a group chat")
    public ResponseEntity<Map<String, Object>> unmuteGroupChat(
            @PathVariable UUID groupId,
            Authentication authentication) {
        
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        
        boolean success = conversationMuteService.unmuteGroupChat(groupId, userId);
        return ResponseEntity.ok(Map.of("success", success));
    }
}

