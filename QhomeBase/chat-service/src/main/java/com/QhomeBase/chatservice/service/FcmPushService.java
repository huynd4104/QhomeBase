package com.QhomeBase.chatservice.service;

import com.QhomeBase.chatservice.dto.MessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.QhomeBase.chatservice.security.UserPrincipal;

import com.QhomeBase.chatservice.repository.GroupMemberRepository;
import com.QhomeBase.chatservice.repository.BlockRepository;
import com.QhomeBase.chatservice.repository.ConversationParticipantRepository;
import com.QhomeBase.chatservice.repository.MessageRepository;
import com.QhomeBase.chatservice.repository.DirectMessageRepository;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmPushService {

    @Value("${customer-interaction.service.url:http://localhost:8087}")
    private String customerInteractionServiceUrl;

    private final WebClient webClient = WebClient.builder().build();
    private final GroupMemberRepository groupMemberRepository;
    private final BlockRepository blockRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final MessageRepository messageRepository;
    private final DirectMessageRepository directMessageRepository;

    /**
     * Send FCM push notification to group members when a new message is received
     * This will call customer-interaction-service to send push notifications
     * @deprecated Use sendChatMessageNotificationToResident() instead for better control
     */
    @Deprecated
    public void sendChatMessageNotification(UUID groupId, MessageResponse message, UUID senderId) {
        try {
            // Get all group members except sender
            List<com.QhomeBase.chatservice.model.GroupMember> members = groupMemberRepository.findByGroupId(groupId);
            
            for (com.QhomeBase.chatservice.model.GroupMember member : members) {
                if (member.getResidentId().equals(senderId)) {
                    continue; // Don't send notification to sender
                }

                // Check if conversation is muted
                if (isGroupMuted(member)) {
                    log.debug("Skipping notification: group {} is muted for resident {}", groupId, member.getResidentId());
                    continue;
                }

                String title = "Tin nhắn mới";
                String excerptMessage = getMessagePreview(message);
                String body = message.getSenderName() != null 
                    ? message.getSenderName() + ": " + excerptMessage
                    : "Bạn có tin nhắn mới";

                // Get unread count for this user
                Long unreadCount = getGroupUnreadCount(groupId, member.getResidentId());

                Map<String, String> data = new HashMap<>();
                data.put("type", "groupMessage");
                data.put("chatId", groupId.toString());
                data.put("groupId", groupId.toString());
                data.put("messageId", message.getId().toString());
                data.put("senderId", senderId.toString());
                data.put("senderName", message.getSenderName() != null ? message.getSenderName() : "");
                data.put("excerptMessage", excerptMessage);
                data.put("unreadCount", String.valueOf(unreadCount));

                // Call customer-interaction-service to send push notification
                sendPushToResident(member.getResidentId(), title, body, data);
            }
        } catch (Exception e) {
            log.error("Error sending FCM push notification for chat message: {}", e.getMessage(), e);
        }
    }

    /**
     * Send FCM push notification to a specific resident for a group chat message
     * This method is called when the recipient is offline (no active WebSocket connection)
     */
    public void sendChatMessageNotificationToResident(UUID groupId, MessageResponse message, UUID senderId, UUID recipientId) {
        try {
            // Get group member info to check mute status
            com.QhomeBase.chatservice.model.GroupMember member = groupMemberRepository
                    .findByGroupIdAndResidentId(groupId, recipientId)
                    .orElse(null);
            
            if (member == null) {
                log.warn("Group member not found for group {} and resident {}", groupId, recipientId);
                return;
            }

            // Check if conversation is muted
            if (isGroupMuted(member)) {
                log.debug("Skipping notification: group {} is muted for resident {}", groupId, recipientId);
                return;
            }

            String title = "Tin nhắn mới";
            String excerptMessage = getMessagePreview(message);
            String body = message.getSenderName() != null 
                ? message.getSenderName() + ": " + excerptMessage
                : "Bạn có tin nhắn mới";

            // Get unread count for this user
            Long unreadCount = getGroupUnreadCount(groupId, recipientId);

            Map<String, String> data = new HashMap<>();
            data.put("type", "groupMessage");
            data.put("chatId", groupId.toString());
            data.put("groupId", groupId.toString());
            data.put("messageId", message.getId().toString());
            data.put("senderId", senderId.toString());
            data.put("senderName", message.getSenderName() != null ? message.getSenderName() : "");
            data.put("excerptMessage", excerptMessage);
            data.put("unreadCount", String.valueOf(unreadCount));

            // Call customer-interaction-service to send push notification
            sendPushToResident(recipientId, title, body, data);
            log.info("📱 [FcmPushService] Sent FCM push notification to resident {} for group {} message", recipientId, groupId);
        } catch (Exception e) {
            log.error("Error sending FCM push notification for chat message to resident {}: {}", recipientId, e.getMessage(), e);
        }
    }

    /**
     * Check if group is muted for a member
     */
    private boolean isGroupMuted(com.QhomeBase.chatservice.model.GroupMember member) {
        // Check old isMuted flag
        if (Boolean.TRUE.equals(member.getIsMuted()) && member.getMuteUntil() == null) {
            return true; // Muted indefinitely (old way)
        }
        
        // Check muteUntil timestamp
        if (member.getMuteUntil() != null) {
            OffsetDateTime now = OffsetDateTime.now();
            if (member.getMuteUntil().isAfter(now)) {
                return true; // Still muted
            }
        }
        
        return false;
    }

    /**
     * Get unread count for a group member
     */
    private Long getGroupUnreadCount(UUID groupId, UUID residentId) {
        try {
            com.QhomeBase.chatservice.model.GroupMember member = groupMemberRepository
                    .findByGroupIdAndResidentId(groupId, residentId)
                    .orElse(null);
            
            if (member == null) {
                return 0L;
            }

            OffsetDateTime lastReadAt = member.getLastReadAt();
            if (lastReadAt == null) {
                lastReadAt = OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, java.time.ZoneOffset.UTC);
            }

            // Count unread messages (excluding messages sent by the current user)
            // Signature: countUnreadMessages(groupId, excludeSenderId, lastReadAt)
            return messageRepository.countUnreadMessages(groupId, residentId, lastReadAt);
        } catch (Exception e) {
            log.error("Error getting unread count for group {} and resident {}: {}", groupId, residentId, e.getMessage());
            return 0L;
        }
    }

    private String getMessagePreview(MessageResponse message) {
        if (message.getIsDeleted() != null && message.getIsDeleted()) {
            return "Tin nhắn đã bị xóa";
        }
        
        if ("IMAGE".equals(message.getMessageType())) {
            return "📷 Đã gửi một hình ảnh";
        }
        
        if ("FILE".equals(message.getMessageType())) {
            return "📎 Đã gửi một tệp";
        }
        
        if (message.getContent() != null && !message.getContent().isEmpty()) {
            String content = message.getContent();
            if (content.length() > 100) {
                return content.substring(0, 100) + "...";
            }
            return content;
        }
        
        return "Tin nhắn mới";
    }

    public void sendPushToResident(UUID residentId, String title, String body, Map<String, String> data) {
        try {
            // Use push-only endpoint which only sends FCM push without saving to notification table
            // This is for chat messages that should appear in chat screen, not in notification list
            String url = customerInteractionServiceUrl + "/api/notifications/push-only";
            
            Map<String, Object> request = new HashMap<>();
            request.put("residentId", residentId.toString());
            request.put("title", title);
            request.put("message", body);
            if (data != null) {
                request.put("data", data);
            }

            webClient
                    .post()
                    .uri(url)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();

            log.debug("Sent FCM push-only notification to resident: {} (not saved to DB)", residentId);
        } catch (Exception e) {
            log.error("Error sending FCM push to resident {}: {}", residentId, e.getMessage(), e);
        }
    }

    /**
     * Check if recipient is blocked by sender
     */
    private boolean isBlocked(UUID senderId, UUID recipientId) {
        return blockRepository.findByBlockerIdAndBlockedId(recipientId, senderId).isPresent();
    }

    /**
     * Send FCM push notification for direct message
     */
    public void sendDirectMessageNotification(UUID recipientId, UUID conversationId, com.QhomeBase.chatservice.dto.DirectMessageResponse message, UUID senderResidentId) {
        // Check if recipient has blocked the sender
        if (isBlocked(senderResidentId, recipientId)) {
            log.debug("Skipping notification: recipient {} has blocked sender {}", recipientId, senderResidentId);
            return;
        }
        
        // Check if conversation is muted
        if (isDirectConversationMuted(conversationId, recipientId)) {
            log.debug("Skipping notification: conversation {} is muted for resident {}", conversationId, recipientId);
            return;
        }
        
        try {
            String excerptMessage = getDirectMessagePreview(message);
            String title = "Tin nhắn mới";
            String body = message.getSenderName() != null 
                ? message.getSenderName() + ": " + excerptMessage
                : "Bạn có tin nhắn mới";

            // Get unread count for this conversation
            Long unreadCount = getDirectConversationUnreadCount(conversationId, recipientId);

            Map<String, String> data = new HashMap<>();
            data.put("type", "directMessage");
            data.put("chatId", conversationId.toString());
            data.put("conversationId", conversationId.toString());
            data.put("messageId", message.getId().toString());
            data.put("senderId", message.getSenderId() != null ? message.getSenderId().toString() : "");
            data.put("senderName", message.getSenderName() != null ? message.getSenderName() : "");
            data.put("excerptMessage", excerptMessage);
            data.put("unreadCount", String.valueOf(unreadCount));

            sendPushToResident(recipientId, title, body, data);
        } catch (Exception e) {
            log.error("Error sending FCM push notification for direct message: {}", e.getMessage(), e);
        }
    }

    /**
     * Check if direct conversation is muted
     */
    private boolean isDirectConversationMuted(UUID conversationId, UUID residentId) {
        try {
            com.QhomeBase.chatservice.model.ConversationParticipant participant = conversationParticipantRepository
                    .findByConversationIdAndResidentId(conversationId, residentId)
                    .orElse(null);
            
            if (participant == null) {
                return false;
            }

            // Check old isMuted flag
            if (Boolean.TRUE.equals(participant.getIsMuted()) && participant.getMuteUntil() == null) {
                return true; // Muted indefinitely (old way)
            }
            
            // Check muteUntil timestamp
            if (participant.getMuteUntil() != null) {
                OffsetDateTime now = OffsetDateTime.now();
                if (participant.getMuteUntil().isAfter(now)) {
                    return true; // Still muted
                }
            }
            
            return false;
        } catch (Exception e) {
            log.error("Error checking if conversation is muted: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get unread count for a direct conversation
     */
    private Long getDirectConversationUnreadCount(UUID conversationId, UUID residentId) {
        try {
            com.QhomeBase.chatservice.model.ConversationParticipant participant = conversationParticipantRepository
                    .findByConversationIdAndResidentId(conversationId, residentId)
                    .orElse(null);
            
            if (participant == null) {
                return 0L;
            }

            OffsetDateTime lastReadAt = participant.getLastReadAt();
            if (lastReadAt == null) {
                lastReadAt = OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, java.time.ZoneOffset.UTC);
            }

            // Count unread messages (excluding messages sent by the current user)
            return directMessageRepository.countUnreadMessages(conversationId, residentId, lastReadAt);
        } catch (Exception e) {
            log.error("Error getting unread count for conversation {} and resident {}: {}", conversationId, residentId, e.getMessage());
            return 0L;
        }
    }

    /**
     * Send FCM push notification for direct invitation
     */
    public void sendDirectInvitationNotification(UUID inviteeId, UUID inviterId, UUID conversationId) {
        try {
            String title = "Lời mời trò chuyện";
            String body = "Bạn có lời mời trò chuyện mới";

            Map<String, String> data = new HashMap<>();
            data.put("type", "DIRECT_INVITATION");
            data.put("conversationId", conversationId.toString());
            data.put("inviterId", inviterId.toString());

            sendPushToResident(inviteeId, title, body, data);
        } catch (Exception e) {
            log.error("Error sending FCM push notification for direct invitation: {}", e.getMessage(), e);
        }
    }

    private String getDirectMessagePreview(com.QhomeBase.chatservice.dto.DirectMessageResponse message) {
        if (message.getIsDeleted() != null && message.getIsDeleted()) {
            return "Tin nhắn đã bị xóa";
        }
        
        if ("IMAGE".equals(message.getMessageType())) {
            return "📷 Đã gửi một hình ảnh";
        }
        
        if ("FILE".equals(message.getMessageType())) {
            return "📎 Đã gửi một tệp";
        }

        if ("AUDIO".equals(message.getMessageType())) {
            return "🎤 Đã gửi một tin nhắn thoại";
        }
        
        if (message.getContent() != null && !message.getContent().isEmpty()) {
            String content = message.getContent();
            if (content.length() > 100) {
                return content.substring(0, 100) + "...";
            }
            return content;
        }
        
        return "Tin nhắn mới";
    }

    private String getCurrentAccessToken() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
                UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
                return principal.token();
            }
        } catch (Exception e) {
            log.debug("Could not get token from SecurityContext: {}", e.getMessage());
        }
        return null;
    }
}

