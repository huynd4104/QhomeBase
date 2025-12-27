package com.QhomeBase.chatservice.service;

import com.QhomeBase.chatservice.dto.CreateMessageRequest;
import com.QhomeBase.chatservice.dto.MessagePagedResponse;
import com.QhomeBase.chatservice.dto.MessageResponse;
import com.QhomeBase.chatservice.model.Group;
import com.QhomeBase.chatservice.model.GroupMember;
import com.QhomeBase.chatservice.model.Message;
import com.QhomeBase.chatservice.repository.GroupMemberRepository;
import com.QhomeBase.chatservice.repository.GroupRepository;
import com.QhomeBase.chatservice.repository.MessageRepository;
import com.QhomeBase.chatservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageRepository messageRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ResidentInfoService residentInfoService;
    private final ChatNotificationService notificationService;
    private final FcmPushService fcmPushService;
    private final com.QhomeBase.chatservice.service.GroupFileService groupFileService;
    private final WebSocketPresenceService presenceService;

    @Value("${marketplace.service.url:http://localhost:8082}")
    private String marketplaceServiceUrl;

    private final WebClient webClient = WebClient.builder().build();

    @Transactional
    public MessageResponse createMessage(UUID groupId, CreateMessageRequest request, UUID userId) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }

        Group group = groupRepository.findActiveGroupById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found: " + groupId));

        // Check if user is a member
        GroupMember member = groupMemberRepository.findByGroupIdAndResidentId(groupId, residentId)
                .orElseThrow(() -> new RuntimeException("You are not a member of this group"));

        // If group was hidden for this user, unhide it when they send a new message
        if (member.getHiddenAt() != null) {
            member.setHiddenAt(null);
            groupMemberRepository.save(member);
            log.info("Unhid group {} for user {} because they sent a new message", groupId, residentId);
        }

        // Validate message type and content
        String messageType = request.getMessageType() != null ? request.getMessageType() : "TEXT";
        if ("TEXT".equals(messageType) && (request.getContent() == null || request.getContent().trim().isEmpty())) {
            throw new RuntimeException("Text message content cannot be empty");
        }
        if ("IMAGE".equals(messageType) && (request.getImageUrl() == null || request.getImageUrl().isEmpty())) {
            throw new RuntimeException("Image message must have imageUrl");
        }
        if ("AUDIO".equals(messageType) && (request.getFileUrl() == null || request.getFileUrl().isEmpty())) {
            throw new RuntimeException("Audio message must have fileUrl");
        }
        if ("FILE".equals(messageType) && (request.getFileUrl() == null || request.getFileUrl().isEmpty())) {
            throw new RuntimeException("File message must have fileUrl");
        }
        if ("MARKETPLACE_POST".equals(messageType)) {
            if (request.getPostId() == null || request.getPostId().isEmpty()) {
                throw new RuntimeException("Marketplace post message must have postId");
            }
            if (request.getPostTitle() == null || request.getPostTitle().isEmpty()) {
                throw new RuntimeException("Marketplace post message must have postTitle");
            }
            // Store marketplace post data as JSON in content field
            String marketplaceData = String.format(
                "{\"postId\":\"%s\",\"postTitle\":\"%s\",\"postThumbnailUrl\":\"%s\",\"postPrice\":%s,\"deepLink\":\"%s\"}",
                request.getPostId() != null ? request.getPostId().replace("\"", "\\\"") : "",
                request.getPostTitle() != null ? request.getPostTitle().replace("\"", "\\\"") : "",
                request.getPostThumbnailUrl() != null ? request.getPostThumbnailUrl().replace("\"", "\\\"") : "",
                request.getPostPrice() != null ? request.getPostPrice() : "null",
                request.getDeepLink() != null ? request.getDeepLink().replace("\"", "\\\"") : ""
            );
            request.setContent(marketplaceData);
            // Use thumbnail as imageUrl for preview
            if (request.getPostThumbnailUrl() != null && !request.getPostThumbnailUrl().isEmpty()) {
                request.setImageUrl(request.getPostThumbnailUrl());
            }
        }

        Message message = Message.builder()
                .group(group)
                .groupId(groupId)
                .senderId(residentId)
                .content(request.getContent())
                .messageType(messageType)
                .imageUrl(request.getImageUrl())
                .fileUrl(request.getFileUrl())
                .fileName(request.getFileName())
                .fileSize(request.getFileSize())
                .mimeType(request.getMimeType())
                .replyToMessageId(request.getReplyToMessageId())
                .isEdited(false)
                .isDeleted(false)
                .build();

        if (request.getReplyToMessageId() != null) {
            Message replyTo = messageRepository.findById(request.getReplyToMessageId())
                    .orElse(null);
            if (replyTo != null && replyTo.getGroupId().equals(groupId)) {
                message.setReplyToMessage(replyTo);
            }
        }

        message = messageRepository.save(message);
        
        // Flush to ensure createdAt is set by @CreationTimestamp
        messageRepository.flush();

        // Update last read time for sender to slightly after the message's createdAt time
        // This ensures the message they just sent is considered as read
        // Add 1 millisecond to avoid any timing edge cases where createdAt equals lastReadAt
        OffsetDateTime createdAt = message.getCreatedAt();
        if (createdAt == null) {
            // Fallback to current time if createdAt is still null (shouldn't happen after flush)
            createdAt = OffsetDateTime.now();
            log.warn("Message {} createdAt was null after flush, using current time", message.getId());
        }
        OffsetDateTime lastReadTime = createdAt.plusNanos(1_000_000); // Add 1 millisecond
        member.setLastReadAt(lastReadTime);
        groupMemberRepository.save(member);
        log.debug("Updated lastReadAt for sender {} in group {} to {}", residentId, groupId, lastReadTime);

        MessageResponse response = toMessageResponse(message);
        
        // Save file metadata if this is a file/image/audio message
        if ("FILE".equals(messageType) || "IMAGE".equals(messageType) || "AUDIO".equals(messageType)) {
            try {
                groupFileService.saveFileMetadata(message);
            } catch (Exception e) {
                log.warn("Failed to save file metadata for message {}: {}", message.getId(), e.getMessage());
                // Don't fail the message creation if file metadata save fails
            }
        }
        
        // Unhide group for all members who had hidden it (when someone sends a new message)
        List<GroupMember> hiddenMembers = groupMemberRepository.findByGroupIdAndHiddenAtIsNotNull(groupId);
        for (GroupMember hiddenMember : hiddenMembers) {
            hiddenMember.setHiddenAt(null);
            groupMemberRepository.save(hiddenMember);
        }
        if (!hiddenMembers.isEmpty()) {
            log.info("Unhid group {} for {} members because a new message was sent", groupId, hiddenMembers.size());
        }
        
        // Notify group members based on their online/offline status
        // Get all group members except sender
        List<GroupMember> allMembers = groupMemberRepository.findByGroupId(groupId);
        
        for (GroupMember groupMember : allMembers) {
            if (groupMember.getResidentId().equals(residentId)) {
                continue; // Don't send notification to sender
            }

            // Check if conversation is muted
            if (isGroupMuted(groupMember)) {
                log.debug("Skipping notification: group {} is muted for resident {}", groupId, groupMember.getResidentId());
                continue;
            }

            // Check if recipient is online (has active WebSocket connection)
            boolean isRecipientOnline = presenceService.isUserOnline(groupMember.getResidentId());

            if (isRecipientOnline) {
                // User is in app (has WebSocket connection) - send realtime notification via WebSocket
                // The WebSocket notification will be sent to all subscribers of /topic/chat/{groupId}
                // We still call notifyNewMessage once, but it will be received by all online members
                log.info("📱 [MessageService] Recipient {} is ONLINE - will receive WebSocket realtime notification (groupId: {})",
                        groupMember.getResidentId(), groupId);
            } else {
                // User is offline (out of app) - send FCM push notification
                fcmPushService.sendChatMessageNotificationToResident(groupId, response, residentId, groupMember.getResidentId());
                log.info("📱 [MessageService] Recipient {} is OFFLINE - sent FCM push notification (groupId: {})",
                        groupMember.getResidentId(), groupId);
            }
        }
        
        // Send WebSocket notification to all subscribers (online users will receive it)
        notificationService.notifyNewMessage(groupId, response);

        return response;
    }

    @Transactional(readOnly = true)
    public MessagePagedResponse getMessages(UUID groupId, UUID userId, int page, int size) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }

        Group group = groupRepository.findActiveGroupById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found: " + groupId));

        // Check if user is a member
        GroupMember member = groupMemberRepository.findByGroupIdAndResidentId(groupId, residentId)
                .orElseThrow(() -> new RuntimeException("You are not a member of this group"));

        Pageable pageable = PageRequest.of(page, size);
        Page<Message> messages = messageRepository.findMessagesByGroupIdOrderByCreatedAtDesc(groupId, pageable);

        // Update last read time
        member.setLastReadAt(OffsetDateTime.now());
        groupMemberRepository.save(member);

        return MessagePagedResponse.builder()
                .content(messages.getContent().stream()
                        .map(this::toMessageResponse)
                        .toList())
                .currentPage(messages.getNumber())
                .pageSize(messages.getSize())
                .totalElements(messages.getTotalElements())
                .totalPages(messages.getTotalPages())
                .hasNext(messages.hasNext())
                .hasPrevious(messages.hasPrevious())
                .isFirst(messages.isFirst())
                .isLast(messages.isLast())
                .build();
    }

    @Transactional
    public MessageResponse updateMessage(UUID groupId, UUID messageId, String content, UUID userId) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found: " + messageId));

        if (!message.getGroupId().equals(groupId)) {
            throw new RuntimeException("Message does not belong to this group");
        }

        // System messages cannot be edited
        if ("SYSTEM".equals(message.getMessageType())) {
            throw new RuntimeException("System messages cannot be edited");
        }

        if (message.getSenderId() == null || !message.getSenderId().equals(residentId)) {
            throw new RuntimeException("You can only edit your own messages");
        }

        if (message.getIsDeleted()) {
            throw new RuntimeException("Cannot edit deleted message");
        }

        if (!"TEXT".equals(message.getMessageType())) {
            throw new RuntimeException("Only text messages can be edited");
        }

        message.setContent(content);
        message.setIsEdited(true);
        message = messageRepository.save(message);

        MessageResponse response = toMessageResponse(message);
        
        // Notify via WebSocket
        notificationService.notifyMessageUpdated(groupId, response);

        return response;
    }

    @Transactional
    public void deleteMessage(UUID groupId, UUID messageId, UUID userId) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found: " + messageId));

        if (!message.getGroupId().equals(groupId)) {
            throw new RuntimeException("Message does not belong to this group");
        }

        // Check if user is sender or admin/moderator
        GroupMember member = groupMemberRepository.findByGroupIdAndResidentId(groupId, residentId)
                .orElseThrow(() -> new RuntimeException("You are not a member of this group"));

        // System messages can only be deleted by admins/moderators
        if ("SYSTEM".equals(message.getMessageType())) {
            boolean canDelete = "ADMIN".equals(member.getRole()) ||
                               "MODERATOR".equals(member.getRole());
            if (!canDelete) {
                throw new RuntimeException("Only admins and moderators can delete system messages");
            }
        } else {
            boolean canDelete = (message.getSenderId() != null && message.getSenderId().equals(residentId)) ||
                               "ADMIN".equals(member.getRole()) ||
                               "MODERATOR".equals(member.getRole());
            if (!canDelete) {
                throw new RuntimeException("You don't have permission to delete this message");
            }
        }


        message.setIsDeleted(true);
        message.setContent(null); // Clear content for deleted messages
        messageRepository.save(message);
        
        // Notify via WebSocket
        notificationService.notifyMessageDeleted(groupId, messageId);
    }

    public Long countUnreadMessages(UUID groupId, OffsetDateTime lastReadAt, UUID excludeSenderId) {
        if (lastReadAt == null) {
            // If never read, count all messages except own messages
            if (excludeSenderId != null) {
                // Use a very old but valid timestamp instead of OffsetDateTime.MIN
                // PostgreSQL timestamp range: 4713 BC to 294276 AD
                // Use year 1970-01-01 as a safe minimum
                OffsetDateTime minValidDate = OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, java.time.ZoneOffset.UTC);
                long count = (long) messageRepository.findNewMessagesByGroupIdAfterExcludingSender(
                    groupId, minValidDate, excludeSenderId).size();
                log.debug("Counted {} unread messages for group {} (never read, excluding sender {})", 
                    count, groupId, excludeSenderId);
                return count;
            }
            long count = messageRepository.countByGroupId(groupId);
            log.debug("Counted {} unread messages for group {} (never read, no exclude)", count, groupId);
            return count;
        }
        // Count messages after lastReadAt, excluding own messages
        // Use >= instead of > to ensure messages created at the same time as lastReadAt are not counted
        // But actually, we want > to exclude the message that set lastReadAt
        if (excludeSenderId != null) {
            long count = (long) messageRepository.findNewMessagesByGroupIdAfterExcludingSender(
                groupId, lastReadAt, excludeSenderId).size();
            log.debug("Counted {} unread messages for group {} (lastReadAt: {}, excluding sender {})", 
                count, groupId, lastReadAt, excludeSenderId);
            return count;
        }
        long count = (long) messageRepository.findNewMessagesByGroupIdAfter(groupId, lastReadAt).size();
        log.debug("Counted {} unread messages for group {} (lastReadAt: {})", count, groupId, lastReadAt);
        return count;
    }

    /**
     * Create a system message in the group
     * @param groupId The group ID
     * @param content The system message content
     * @return The created message response
     */
    @Transactional
    public MessageResponse createSystemMessage(UUID groupId, String content) {
        Group group = groupRepository.findActiveGroupById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found: " + groupId));

        Message message = Message.builder()
                .group(group)
                .groupId(groupId)
                .senderId(null) // System messages have no sender
                .content(content)
                .messageType("SYSTEM")
                .isEdited(false)
                .isDeleted(false)
                .build();

        message = messageRepository.save(message);
        messageRepository.flush();

        MessageResponse response = toMessageResponse(message);
        
        // Notify via WebSocket
        notificationService.notifyNewMessage(groupId, response);
        
        return response;
    }

    @Transactional
    public void markMessagesAsRead(UUID groupId, UUID userId) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }

        GroupMember member = groupMemberRepository.findByGroupIdAndResidentId(groupId, residentId)
                .orElseThrow(() -> new RuntimeException("You are not a member of this group"));

        // Update last read time to now
        member.setLastReadAt(OffsetDateTime.now());
        groupMemberRepository.save(member);
    }

    public Long getUnreadCount(UUID groupId, UUID userId) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            return 0L;
        }

        GroupMember member = groupMemberRepository.findByGroupIdAndResidentId(groupId, residentId)
                .orElse(null);
        
        if (member == null) {
            return 0L;
        }

        // Exclude messages sent by the current user when counting unread
        return countUnreadMessages(groupId, member.getLastReadAt(), residentId);
    }

    private MessageResponse toMessageResponse(Message message) {
        Map<String, Object> senderInfo = null;
        String senderName = null;
        String senderAvatarUrl = null;
        
        // System messages have no sender
        if (message.getSenderId() != null && !"SYSTEM".equals(message.getMessageType())) {
            senderInfo = residentInfoService.getResidentInfo(message.getSenderId());
            senderName = senderInfo != null ? (String) senderInfo.get("fullName") : null;
            senderAvatarUrl = senderInfo != null ? (String) senderInfo.get("avatarUrl") : null;
        }

        MessageResponse.MessageResponseBuilder builder = MessageResponse.builder()
                .id(message.getId())
                .groupId(message.getGroupId())
                .senderId(message.getSenderId())
                .senderName(senderName)
                .senderAvatar(senderAvatarUrl)
                .content(message.getContent())
                .messageType(message.getMessageType())
                .imageUrl(message.getImageUrl())
                .fileUrl(message.getFileUrl())
                .fileName(message.getFileName())
                .fileSize(message.getFileSize())
                .mimeType(message.getMimeType())
                .replyToMessageId(message.getReplyToMessageId())
                .isEdited(message.getIsEdited())
                .isDeleted(message.getIsDeleted())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt());
        
        // Parse marketplace_post data from content if messageType is MARKETPLACE_POST
        if ("MARKETPLACE_POST".equals(message.getMessageType()) && message.getContent() != null) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                java.util.Map<String, Object> marketplaceData = objectMapper.readValue(message.getContent(), java.util.Map.class);
                String postId = (String) marketplaceData.get("postId");
                builder.postId(postId);
                builder.postTitle((String) marketplaceData.get("postTitle"));
                builder.postThumbnailUrl((String) marketplaceData.get("postThumbnailUrl"));
                Object priceObj = marketplaceData.get("postPrice");
                if (priceObj != null && !"null".equals(priceObj.toString())) {
                    if (priceObj instanceof Number) {
                        builder.postPrice(((Number) priceObj).doubleValue());
                    } else {
                        builder.postPrice(Double.parseDouble(priceObj.toString()));
                    }
                }
                builder.deepLink((String) marketplaceData.get("deepLink"));
                
                // Check post status from marketplace service
                if (postId != null && !postId.isEmpty()) {
                    String postStatus = checkPostStatus(postId);
                    builder.postStatus(postStatus);
                }
            } catch (Exception e) {
                log.warn("Failed to parse marketplace_post data: {}", e.getMessage());
            }
        }

        // Add reply message if exists
        if (message.getReplyToMessage() != null) {
            builder.replyToMessage(toMessageResponse(message.getReplyToMessage()));
        }

        return builder.build();
    }

    /**
     * Check post status from marketplace service
     * Returns "ACTIVE", "SOLD", "DELETED", or null if check fails
     */
    private String checkPostStatus(String postId) {
        try {
            String accessToken = getCurrentAccessToken();
            String url = UriComponentsBuilder
                    .fromUriString(marketplaceServiceUrl)
                    .path("/api/marketplace/posts/{postId}")
                    .buildAndExpand(postId)
                    .toUriString();
            
            org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec<?> requestSpec = webClient
                    .get()
                    .uri(url);
            
            // Add authorization header if access token is available
            if (accessToken != null && !accessToken.isEmpty()) {
                requestSpec = requestSpec.header("Authorization", "Bearer " + accessToken);
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = (Map<String, Object>) requestSpec
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            
            if (response != null && response.get("status") != null) {
                return response.get("status").toString();
            }
        } catch (Exception e) {
            log.debug("Failed to check post status for postId {}: {}", postId, e.getMessage());
            // If post not found or error, assume DELETED
            return "DELETED";
        }
        return null;
    }
    
    private String getCurrentAccessToken() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
                UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
                String token = principal.token();
                if (token != null && !token.isEmpty()) {
                    return token;
                }
            }
        } catch (Exception e) {
            log.debug("Could not get token from SecurityContext: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Delete all messages sent by a user in a group from a specific time onwards
     */
    @Transactional
    public void deleteUserMessagesFromGroup(UUID groupId, UUID senderId, OffsetDateTime fromTime) {
        List<Message> messages = messageRepository.findMessagesByGroupIdAndSenderIdFromTime(
            groupId, senderId, fromTime
        );
        
        for (Message message : messages) {
            message.setIsDeleted(true);
            message.setContent(null); // Clear content for deleted messages
            // Keep file/image URLs for now, but mark as deleted
        }
        
        messageRepository.saveAll(messages);
        log.info("Deleted {} messages from user {} in group {} from {}", 
                messages.size(), senderId, groupId, fromTime);
    }

    /**
     * Delete all files/images uploaded by a user in a group from a specific time onwards
     */
    @Transactional
    public void deleteUserFilesFromGroup(UUID groupId, UUID senderId, OffsetDateTime fromTime) {
        // Use GroupFileService to get files and delete them
        List<com.QhomeBase.chatservice.model.GroupFile> files = 
            groupFileService.findFilesByGroupIdAndSenderIdFromTime(groupId, senderId, fromTime);
        
        // Delete the GroupFile records (soft delete - actual files can be cleaned up separately)
        for (com.QhomeBase.chatservice.model.GroupFile file : files) {
            groupFileService.deleteFileMetadata(file.getId());
        }
        
        log.info("Deleted {} files from user {} in group {} from {}", 
                files.size(), senderId, groupId, fromTime);
    }

    /**
     * Check if group is muted for a member
     */
    private boolean isGroupMuted(GroupMember member) {
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
}

