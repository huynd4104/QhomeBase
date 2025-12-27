package com.QhomeBase.chatservice.service;

import com.QhomeBase.chatservice.dto.*;
import com.QhomeBase.chatservice.model.*;
import com.QhomeBase.chatservice.repository.*;
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
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DirectChatService {

    private final DirectMessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final DirectChatFileRepository fileRepository;
    private final BlockRepository blockRepository;
    private final ResidentInfoService residentInfoService;
    private final ChatNotificationService notificationService;
    private final FcmPushService fcmPushService;
    private final FriendshipService friendshipService;
    private final DirectMessageDeletionRepository messageDeletionRepository;
    private final WebSocketPresenceService presenceService;

    @Value("${marketplace.service.url:http://localhost:8082}")
    private String marketplaceServiceUrl;

    private final WebClient webClient = WebClient.builder().build();

    private String getCurrentAccessToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal.token();
        }
        return null;
    }

    /**
     * Get or create conversation between two users
     */
    @Transactional(readOnly = true)
    public Conversation getOrCreateConversation(UUID userId1, UUID userId2) {
        // Ensure participant1_id < participant2_id for uniqueness
        UUID participant1Id = userId1.compareTo(userId2) < 0 ? userId1 : userId2;
        UUID participant2Id = userId1.compareTo(userId2) < 0 ? userId2 : userId1;

        return conversationRepository
                .findConversationBetweenParticipants(participant1Id, participant2Id)
                .orElse(null);
    }

    /**
     * Get all active conversations for a user
     */
    @Transactional(readOnly = true)
    public List<ConversationResponse> getConversations(UUID userId) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            log.error("❌ [DirectChatService] getConversations - Resident not found for userId: {}", userId);
            throw new RuntimeException("Resident not found for user: " + userId);
        }

        log.info("🔍 [DirectChatService] getConversations - userId: {}, residentId: {}", userId, residentId);
        
        // Get conversations including ACTIVE, BLOCKED, and PENDING (but not DELETED)
        // This allows blocked conversations to still be visible in the list
        List<Conversation> conversations = conversationRepository
                .findVisibleConversationsByUserId(residentId);
        

        List<ConversationResponse> filteredConversations = conversations.stream()
                .filter(conv -> {
                    log.debug("🔍 [DirectChatService] Filtering conversation: id={}, status={}, participant1Id={}, participant2Id={}", 
                            conv.getId(), conv.getStatus(), conv.getParticipant1Id(), conv.getParticipant2Id());
                    
                    // Filter out DELETED conversations (both participants have hidden)
                    if ("DELETED".equals(conv.getStatus())) {
                        log.debug("❌ [DirectChatService] Filtered out DELETED conversation: {}", conv.getId());
                        return false;
                    }
                    
                    // IMPORTANT: Show ACTIVE, BLOCKED, and LOCKED conversations (but not PENDING)
                    // - ACTIVE: Both users are friends and can chat
                    // - BLOCKED: One user blocked the other (still visible to show unblock option)
                    // - LOCKED: Not friends anymore but conversation exists (can view history, cannot send)
                    // - PENDING conversations should NOT appear in chat list until invitation is accepted
                    if ("PENDING".equals(conv.getStatus())) {
                        log.debug("❌ [DirectChatService] Filtered out PENDING conversation: {}", conv.getId());
                        return false;
                    }
                    
                    // Filter out conversations hidden by current user
                    ConversationParticipant participant = participantRepository
                            .findByConversationIdAndResidentId(conv.getId(), residentId)
                            .orElse(null);
                    
                    if (participant == null) {
                        log.warn("⚠️ [DirectChatService] Participant not found for conversation: {}, residentId: {}", conv.getId(), residentId);
                        return false; // Don't include if participant not found
                    }
                    
                    boolean isHidden = Boolean.TRUE.equals(participant.getIsHidden());
                    log.debug("🔍 [DirectChatService] Conversation: {}, participant.isHidden: {}, hiddenAt: {}", 
                            conv.getId(), isHidden, participant.getHiddenAt());
                    
                    if (isHidden) {
                        log.debug("❌ [DirectChatService] Filtered out HIDDEN conversation: {} for residentId: {}", conv.getId(), residentId);
                        return false;
                    }
                    
                    log.debug("✅ [DirectChatService] Including conversation: {}", conv.getId());
                    return true;
                })
                .map(conv -> {
                    log.debug("📝 [DirectChatService] Converting conversation to response: {}", conv.getId());
                    return toConversationResponse(conv, residentId, accessToken);
                })
                .collect(Collectors.toList());
        
        log.info("✅ [DirectChatService] getConversations returning {} filtered conversations for userId: {}, residentId: {}", 
                filteredConversations.size(), userId, residentId);
        
        return filteredConversations;
    }

    /**
     * Get conversation by ID
     */
    @Transactional(readOnly = true)
    public ConversationResponse getConversation(UUID conversationId, UUID userId) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        // Check if user is a participant
        if (!conversation.isParticipant(residentId)) {
            throw new RuntimeException("You are not a participant in this conversation");
        }

        // Check if conversation is DELETED (both participants have hidden it)
        if ("DELETED".equals(conversation.getStatus())) {
            throw new RuntimeException("Conversation has been deleted by both participants.");
        }

        // Check if conversation is hidden for this user
        ConversationParticipant participant = participantRepository
                .findByConversationIdAndResidentId(conversationId, residentId)
                .orElse(null);
        if (participant != null && Boolean.TRUE.equals(participant.getIsHidden())) {
            throw new RuntimeException("Conversation is hidden. You cannot view this conversation.");
        }

        // Don't block access to conversation - allow user to see messages even if blocked
        // Frontend will handle showing "User not found" message in input area if blocked

        return toConversationResponse(conversation, residentId, accessToken);
    }

    /**
     * Create a direct message
     */
    @Transactional
    public DirectMessageResponse createMessage(
            UUID conversationId,
            CreateDirectMessageRequest request,
            UUID userId) {
        
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        // Check if user is a participant
        if (!conversation.isParticipant(residentId)) {
            throw new RuntimeException("You are not a participant in this conversation");
        }

        // Check conversation status and permissions to send messages
        log.info("=== sendMessage ===");
        log.info("Conversation ID: {}", conversationId);
        log.info("Conversation status: {}", conversation.getStatus());
        log.info("Resident ID: {}", residentId);
        
        UUID otherParticipantId = conversation.getOtherParticipantId(residentId);
        
        // Check conversation status - only ACTIVE conversations allow sending messages
        if (!"ACTIVE".equals(conversation.getStatus())) {
            String errorMessage;
            if ("BLOCKED".equals(conversation.getStatus())) {
                // Check who blocked whom to provide appropriate error message
                boolean currentUserBlockedOther = blockRepository.findByBlockerIdAndBlockedId(residentId, otherParticipantId).isPresent();
                if (currentUserBlockedOther) {
                    errorMessage = "Bạn đã chặn người dùng này";
                } else {
                    errorMessage = "Người dùng hiện không hoạt động";
                }
            } else if ("LOCKED".equals(conversation.getStatus())) {
                errorMessage = "Bạn chưa gửi lời mời trò chuyện";
            } else {
                errorMessage = "Không thể gửi tin nhắn. Trạng thái: " + conversation.getStatus();
            }
            log.error("Conversation is not active. Status: {}, error: {}", conversation.getStatus(), errorMessage);
            throw new RuntimeException(errorMessage);
        }
        
        // Check bidirectional blocking - if either party has blocked the other, prevent sending messages
        // If A blocks B, then:
        // - A cannot send messages to B (A sees "Bạn đã chặn người dùng này")
        // - B cannot send messages to A (B sees "Người dùng hiện không hoạt động")
        boolean currentUserBlockedOther = blockRepository.findByBlockerIdAndBlockedId(residentId, otherParticipantId).isPresent();
        boolean otherBlockedCurrentUser = blockRepository.findByBlockerIdAndBlockedId(otherParticipantId, residentId).isPresent();
        
        if (currentUserBlockedOther) {
            log.warn("Current user {} has blocked other participant {}. Cannot send messages.", residentId, otherParticipantId);
            throw new RuntimeException("Bạn đã chặn người dùng này");
        }
        
        if (otherBlockedCurrentUser) {
            log.warn("Other participant {} has blocked current user {}. Cannot send messages.", otherParticipantId, residentId);
            throw new RuntimeException("Người dùng hiện không hoạt động");
        }
        
        // Check if users are friends - if not, prevent sending messages
        boolean areFriends = friendshipService.areFriends(residentId, otherParticipantId);
        if (!areFriends) {
            log.warn("Users {} and {} are not friends. Cannot send messages.", residentId, otherParticipantId);
            throw new RuntimeException("Bạn chưa gửi lời mời trò chuyện");
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

        // Create message - @CreationTimestamp will automatically set createdAt when persisted
        DirectMessage message = DirectMessage.builder()
                .conversation(conversation)
                .conversationId(conversationId)
                .senderId(residentId)
                .content(request.getContent())
                .messageType(messageType)
                .imageUrl(request.getImageUrl())
                .fileUrl(request.getFileUrl())
                .fileName(request.getFileName())
                .fileSize(request.getFileSize())
                .mimeType(request.getMimeType())
                .replyToMessageId(request.getReplyToMessageId())
                .build();

        // Save message - @CreationTimestamp will set createdAt to current server time
        OffsetDateTime beforeSave = OffsetDateTime.now();
        message = messageRepository.save(message);
        messageRepository.flush(); // Ensure timestamp is persisted immediately
        OffsetDateTime afterSave = OffsetDateTime.now();
        
        // Log timestamp for debugging - verify it's set correctly
        OffsetDateTime messageCreatedAt = message.getCreatedAt();
        if (messageCreatedAt != null) {
            log.info("📅 Message timestamp - Created at: {} (offset: {}), Before save: {}, After save: {}", 
                    messageCreatedAt,
                    messageCreatedAt.getOffset(),
                    beforeSave,
                    afterSave);
        } else {
            log.error("⚠️ Message createdAt is null after save! This should not happen with @CreationTimestamp");
        }

        // Update conversation updated_at
        conversation.setUpdatedAt(OffsetDateTime.now());
        conversationRepository.save(conversation);

        // Unhide conversation for sender if it was hidden (when they send a new message)
        ConversationParticipant senderParticipant = participantRepository
                .findByConversationIdAndResidentId(conversationId, residentId)
                .orElse(null);
        if (senderParticipant != null && Boolean.TRUE.equals(senderParticipant.getIsHidden())) {
            senderParticipant.setIsHidden(false);
            senderParticipant.setHiddenAt(null);
            participantRepository.save(senderParticipant);
            log.info("Conversation {} unhidden for sender {} because they sent a new message", conversationId, residentId);
        }
        
        // Update sender's lastReadAt to mark message as read
        if (senderParticipant != null) {
            OffsetDateTime createdAt = message.getCreatedAt();
            if (createdAt == null) {
                createdAt = OffsetDateTime.now();
            }
            senderParticipant.setLastReadAt(createdAt.plusNanos(1_000_000));
            participantRepository.save(senderParticipant);
        }

        DirectMessageResponse response = toDirectMessageResponse(message, accessToken);

        // Save file metadata if this is a file/image/audio message
        if ("FILE".equals(messageType) || "IMAGE".equals(messageType) || "AUDIO".equals(messageType)) {
            try {
                saveDirectChatFileMetadata(message);
            } catch (Exception e) {
                log.warn("Failed to save file metadata for message {}: {}", message.getId(), e.getMessage());
            }
        }

        // Unhide conversation for all participants who had hidden it (when someone sends a new message)
        List<ConversationParticipant> allParticipants = participantRepository.findByConversationId(conversationId);
        for (ConversationParticipant participant : allParticipants) {
            if (Boolean.TRUE.equals(participant.getIsHidden())) {
                participant.setIsHidden(false);
                participant.setHiddenAt(null);
                // Reset lastReadAt to null so the new message is considered as the first message
                participant.setLastReadAt(null);
                participantRepository.save(participant);
                log.info("Conversation {} unhidden for participant {} (new message received). lastReadAt reset to null.", conversationId, participant.getResidentId());
            }
        }
        
        // If conversation was DELETED (both participants had hidden it), reset to ACTIVE
        if ("DELETED".equals(conversation.getStatus())) {
            conversation.setStatus("ACTIVE");
            conversationRepository.save(conversation);
            log.info("Conversation {} reset from DELETED to ACTIVE (new message received)", conversationId);
        }

        // Check if recipient is online (has active WebSocket connection)
        boolean isRecipientOnline = presenceService.isUserOnline(otherParticipantId);
        
        if (isRecipientOnline) {
            // User is in app (has WebSocket connection) - send realtime notification via WebSocket
            // This covers both cases:
            // 1. User is in chat screen - will receive WebSocket notification
            // 2. User is in app but on home screen - will receive WebSocket notification (realtime notification)
            // Note: If user is online but not subscribed to conversation topic, they won't receive WebSocket notification
            // but that's handled by client-side subscription logic
            notificationService.notifyDirectMessage(conversationId, response);
            log.info("📱 [DirectChatService] Recipient {} is ONLINE - sent WebSocket realtime notification (conversationId: {})", 
                    otherParticipantId, conversationId);
        } else {
            // User is offline (out of app) - send FCM push notification
            // This ensures user receives notification even when app is closed or in background
        fcmPushService.sendDirectMessageNotification(otherParticipantId, conversationId, response, residentId);
            log.info("📱 [DirectChatService] Recipient {} is OFFLINE - sent FCM push notification (conversationId: {})", 
                    otherParticipantId, conversationId);
        }

        return response;
    }

    /**
     * Get messages in a conversation with pagination
     */
    @Transactional
    public DirectMessagePagedResponse getMessages(UUID conversationId, UUID userId, int page, int size) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }

        log.info("📥 [DirectChatService] getMessages called - conversationId: {}, userId: {}, residentId: {}, page: {}, size: {}", 
                conversationId, userId, residentId, page, size);

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        // Check if user is a participant
        if (!conversation.isParticipant(residentId)) {
            throw new RuntimeException("You are not a participant in this conversation");
        }

        // Check if conversation is DELETED (both participants have hidden it)
        if ("DELETED".equals(conversation.getStatus())) {
            throw new RuntimeException("Conversation has been deleted by both participants.");
        }

        // Check if conversation is hidden for this user
        ConversationParticipant participant = participantRepository
                .findByConversationIdAndResidentId(conversationId, residentId)
                .orElse(null);
        if (participant != null && Boolean.TRUE.equals(participant.getIsHidden())) {
            throw new RuntimeException("Conversation is hidden. You cannot view messages.");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<DirectMessage> messages = messageRepository
                .findByConversationIdOrderByCreatedAtDesc(conversationId, pageable);

        // Get all deletion records for these messages
        List<UUID> messageIds = messages.getContent().stream()
                .map(DirectMessage::getId)
                .collect(Collectors.toList());
        
        List<DirectMessageDeletion> deletions = messageDeletionRepository.findByMessageIdIn(messageIds);
        
        // Get other participant ID
        UUID otherParticipantId = conversation.getOtherParticipantId(residentId);
        
        // Check bidirectional blocking - messages cannot be sent when blocked, so no need to filter
        // But we keep the check for logging/debugging purposes
        Optional<Block> senderBlockedRecipient = blockRepository.findByBlockerIdAndBlockedId(otherParticipantId, residentId);
        Optional<Block> recipientBlockedSender = blockRepository.findByBlockerIdAndBlockedId(residentId, otherParticipantId);
        
        if (senderBlockedRecipient.isPresent() || recipientBlockedSender.isPresent()) {
            log.debug("Block relationship exists between {} and {} - messages cannot be sent, showing all existing messages", 
                    residentId, otherParticipantId);
        }
        
        // No need to filter messages - blocking prevents sending new messages, so all existing messages are valid
        List<DirectMessage> allMessages = messages.getContent();

        // Update last read time (participant already loaded above)
        if (participant != null) {
            OffsetDateTime oldLastReadAt = participant.getLastReadAt();
            OffsetDateTime newLastReadAt = OffsetDateTime.now();
            participant.setLastReadAt(newLastReadAt);
            participantRepository.save(participant);
            log.info("✅ [DirectChatService] Updated lastReadAt for participant - conversationId: {}, residentId: {}, oldLastReadAt: {}, newLastReadAt: {}", 
                    conversationId, residentId, oldLastReadAt, newLastReadAt);
        } else {
            log.warn("⚠️ [DirectChatService] Participant is null, cannot update lastReadAt - conversationId: {}, residentId: {}", 
                    conversationId, residentId);
        }

        log.info("📤 [DirectChatService] Returning {} messages for conversationId: {}", 
                allMessages.size(), conversationId);

        // Create a map of messageId -> deletion for quick lookup
        Map<UUID, DirectMessageDeletion> deletionMap = deletions.stream()
                .filter(d -> d.getDeletedByUserId().equals(residentId) || 
                            d.getDeleteType() == DirectMessageDeletion.DeleteType.FOR_EVERYONE)
                .collect(Collectors.toMap(
                    DirectMessageDeletion::getMessageId,
                    d -> d,
                    (d1, d2) -> {
                        // If both FOR_ME and FOR_EVERYONE exist, prefer FOR_EVERYONE
                        if (d1.getDeleteType() == DirectMessageDeletion.DeleteType.FOR_EVERYONE) {
                            return d1;
                        }
                        return d2;
                    }
                ));

        // Create a custom page response with all messages (including deleted ones)
        return DirectMessagePagedResponse.builder()
                .content(allMessages.stream()
                        .map(msg -> {
                            DirectMessageDeletion deletion = deletionMap.get(msg.getId());
                            return toDirectMessageResponse(msg, accessToken, deletion);
                        })
                        .collect(Collectors.toList()))
                .currentPage(messages.getNumber())
                .pageSize(messages.getSize())
                .totalElements(messages.getTotalElements())
                .totalPages(messages.getTotalPages())
                .hasNext(messages.hasNext())
                .hasPrevious(messages.hasPrevious())
                .first(messages.isFirst())
                .last(messages.isLast())
                .build();
    }

    /**
     * Update/edit a direct message
     * Only the sender can edit their own TEXT messages
     */
    @Transactional
    public DirectMessageResponse updateMessage(UUID conversationId, UUID messageId, String content, UUID userId) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }

        // Check conversation exists and user is a participant
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        if (!conversation.isParticipant(residentId)) {
            throw new RuntimeException("You are not a participant in this conversation");
        }

        // Find message
        DirectMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        // Verify message belongs to this conversation
        if (!message.getConversationId().equals(conversationId)) {
            throw new RuntimeException("Message does not belong to this conversation");
        }

        // System messages cannot be edited
        if ("SYSTEM".equals(message.getMessageType())) {
            throw new RuntimeException("System messages cannot be edited");
        }

        // Only sender can edit their own message
        if (message.getSenderId() == null || !message.getSenderId().equals(residentId)) {
            throw new RuntimeException("You can only edit your own messages");
        }

        // Cannot edit deleted messages
        if (Boolean.TRUE.equals(message.getIsDeleted())) {
            throw new RuntimeException("Cannot edit deleted message");
        }

        // Only text messages can be edited
        if (!"TEXT".equals(message.getMessageType())) {
            throw new RuntimeException("Only text messages can be edited");
        }

        // Update message
        message.setContent(content);
        message.setIsEdited(true);
        message = messageRepository.save(message);

        DirectMessageResponse response = toDirectMessageResponse(message, accessToken);
        
        // Notify via WebSocket
        notificationService.notifyDirectMessageUpdated(conversationId, response);

        return response;
    }

    @Transactional
    public void deleteMessage(UUID conversationId, UUID messageId, UUID userId, String deleteType) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }

        // Validate deleteType
        DirectMessageDeletion.DeleteType type;
        try {
            type = DirectMessageDeletion.DeleteType.valueOf(deleteType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid deleteType. Must be FOR_ME or FOR_EVERYONE");
        }

        // Check conversation exists and user is a participant
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        if (!conversation.isParticipant(residentId)) {
            throw new RuntimeException("You are not a participant in this conversation");
        }

        // Find message
        DirectMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        // Verify message belongs to this conversation
        if (!message.getConversationId().equals(conversationId)) {
            throw new RuntimeException("Message does not belong to this conversation");
        }

        // Only sender can delete their own message
        if (message.getSenderId() == null || !message.getSenderId().equals(residentId)) {
            throw new RuntimeException("You can only delete your own messages");
        }

        // Check if already deleted with same type
        List<DirectMessageDeletion> existingDeletions = messageDeletionRepository
                .findByMessageIdAndDeletedByUserId(messageId, residentId);
        boolean alreadyDeletedForMe = existingDeletions.stream()
                .anyMatch(d -> d.getDeleteType() == DirectMessageDeletion.DeleteType.FOR_ME);
        boolean alreadyDeletedForEveryone = existingDeletions.stream()
                .anyMatch(d -> d.getDeleteType() == DirectMessageDeletion.DeleteType.FOR_EVERYONE);

        if (type == DirectMessageDeletion.DeleteType.FOR_EVERYONE && alreadyDeletedForEveryone) {
            log.warn("Message {} already deleted for everyone", messageId);
            return;
        }
        if (type == DirectMessageDeletion.DeleteType.FOR_ME && alreadyDeletedForMe) {
            log.warn("Message {} already deleted for user {}", messageId, residentId);
            return;
        }

        // Create deletion record
        DirectMessageDeletion deletion = DirectMessageDeletion.builder()
                .messageId(messageId)
                .deletedByUserId(residentId)
                .deleteType(type)
                .deletedAt(OffsetDateTime.now())
                .build();
        messageDeletionRepository.save(deletion);

        // If deleting for everyone, also mark the message as deleted (legacy support)
        if (type == DirectMessageDeletion.DeleteType.FOR_EVERYONE) {
            message.setIsDeleted(true);
            messageRepository.save(message);
        }

        log.info("Message {} deleted by user {} with type {}", messageId, residentId, type);

        // Notify via WebSocket
        DirectMessageResponse response = toDirectMessageResponse(message, accessToken);
        notificationService.notifyDirectMessageDeleted(conversationId, messageId, response);
    }

    /**
     * Count unread messages in a conversation
     */
    @Transactional(readOnly = true)
    public Long countUnreadMessages(UUID conversationId, UUID userId) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            return 0L;
        }

        ConversationParticipant participant = participantRepository
                .findByConversationIdAndResidentId(conversationId, residentId)
                .orElse(null);

        if (participant == null) {
            return 0L;
        }

        OffsetDateTime lastReadAt = participant.getLastReadAt();
        if (lastReadAt == null) {
            // If never read, count all messages except system messages
            lastReadAt = OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, java.time.ZoneOffset.UTC);
        }

        return messageRepository.countUnreadMessages(conversationId, residentId, lastReadAt);
    }

    /**
     * Save file metadata for direct chat
     */
    @Transactional
    public DirectChatFile saveDirectChatFileMetadata(DirectMessage message) {
        if (!"FILE".equals(message.getMessageType()) && 
            !"IMAGE".equals(message.getMessageType()) && 
            !"AUDIO".equals(message.getMessageType())) {
            return null;
        }

        // Determine file URL and type
        String fileUrl = null;
        String fileType = null;
        if ("IMAGE".equals(message.getMessageType())) {
            fileUrl = message.getImageUrl();
            fileType = "IMAGE";
        } else if ("FILE".equals(message.getMessageType()) || "AUDIO".equals(message.getMessageType())) {
            fileUrl = message.getFileUrl();
            fileType = "AUDIO".equals(message.getMessageType()) ? "AUDIO" : "DOCUMENT";
        }

        if (fileUrl == null || fileUrl.isEmpty()) {
            return null;
        }

        // Infer mimeType if not provided
        String mimeType = message.getMimeType();
        if (mimeType == null || mimeType.isEmpty()) {
            mimeType = inferMimeType(message.getFileName(), fileType);
        }

        DirectChatFile file = DirectChatFile.builder()
                .conversation(message.getConversation())
                .conversationId(message.getConversationId())
                .message(message)
                .messageId(message.getId())
                .senderId(message.getSenderId())
                .fileName(message.getFileName() != null ? message.getFileName() : "file")
                .fileSize(message.getFileSize() != null ? message.getFileSize() : 0L)
                .fileType(fileType)
                .mimeType(mimeType)
                .fileUrl(fileUrl)
                .build();

        return fileRepository.save(file);
    }

    private String inferMimeType(String fileName, String fileType) {
        if (fileName == null) return "application/octet-stream";
        
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) return "image/jpeg";
        if (lowerName.endsWith(".png")) return "image/png";
        if (lowerName.endsWith(".gif")) return "image/gif";
        if (lowerName.endsWith(".pdf")) return "application/pdf";
        if (lowerName.endsWith(".doc") || lowerName.endsWith(".docx")) return "application/msword";
        if (lowerName.endsWith(".xls") || lowerName.endsWith(".xlsx")) return "application/vnd.ms-excel";
        if (lowerName.endsWith(".zip")) return "application/zip";
        if (lowerName.endsWith(".mp3")) return "audio/mpeg";
        if (lowerName.endsWith(".mp4")) return "video/mp4";
        
        return "application/octet-stream";
    }

    /**
     * Get all friends for current user
     */
    @Transactional(readOnly = true)
    public List<FriendResponse> getFriends(UUID userId) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }

        List<com.QhomeBase.chatservice.model.Friendship> friendships = friendshipService.getActiveFriendships(residentId);

        return friendships.stream()
                .map(friendship -> {
                    UUID friendId = friendship.getOtherUserId(residentId);
                    Map<String, Object> friendInfo = residentInfoService.getResidentInfo(friendId);
                    
                    // Get friend name - try multiple fields like other services do
                    String friendName = "Unknown";
                    if (friendInfo != null) {
                        Object nameObj = friendInfo.get("fullName");
                        if (nameObj != null) {
                            friendName = nameObj.toString();
                        } else {
                            nameObj = friendInfo.get("name");
                            if (nameObj != null) {
                                friendName = nameObj.toString();
                            } else {
                                // Try firstName + lastName
                                Object firstNameObj = friendInfo.get("firstName");
                                Object lastNameObj = friendInfo.get("lastName");
                                if (firstNameObj != null || lastNameObj != null) {
                                    String firstName = firstNameObj != null ? firstNameObj.toString() : "";
                                    String lastName = lastNameObj != null ? lastNameObj.toString() : "";
                                    friendName = (firstName + " " + lastName).trim();
                                    if (friendName.isEmpty()) {
                                        friendName = "Unknown";
                                    }
                                }
                            }
                        }
                    }
                    
                    String friendPhone = friendInfo != null ? (String) friendInfo.getOrDefault("phone", "") : "";

                    // Check if conversation exists
                    Conversation conversation = conversationRepository
                            .findConversationBetweenParticipants(residentId, friendId)
                            .orElse(null);

                    UUID conversationId = conversation != null ? conversation.getId() : null;
                    Boolean hasActiveConversation = conversation != null && "ACTIVE".equals(conversation.getStatus());

                    return FriendResponse.builder()
                            .friendId(friendId)
                            .friendName(friendName)
                            .friendPhone(friendPhone)
                            .conversationId(conversationId)
                            .hasActiveConversation(hasActiveConversation)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private ConversationResponse toConversationResponse(Conversation conversation, UUID currentUserId, String accessToken) {
        UUID otherParticipantId = conversation.getOtherParticipantId(currentUserId);
        String otherParticipantName = residentInfoService.getResidentName(otherParticipantId, accessToken);
        String participant1Name = residentInfoService.getResidentName(conversation.getParticipant1Id(), accessToken);
        String participant2Name = residentInfoService.getResidentName(conversation.getParticipant2Id(), accessToken);

        // Check bidirectional blocking status
        Optional<Block> senderBlockedRecipient = blockRepository.findByBlockerIdAndBlockedId(otherParticipantId, currentUserId);
        Optional<Block> recipientBlockedSender = blockRepository.findByBlockerIdAndBlockedId(currentUserId, otherParticipantId);
        
        // Get last message - blocking prevents sending new messages, so get last message normally
        DirectMessage lastMessage = messageRepository
                .findByConversationIdOrderByCreatedAtDesc(conversation.getId(), PageRequest.of(0, 1))
                .getContent()
                .stream()
                .findFirst()
                .orElse(null);

        // Get unread count - filter out blocked messages
        ConversationParticipant participant = participantRepository
                .findByConversationIdAndResidentId(conversation.getId(), currentUserId)
                .orElse(null);
        Long unreadCount = 0L;
        OffsetDateTime lastReadAt = null;
        if (participant != null) {
            lastReadAt = participant.getLastReadAt();
            if (lastReadAt == null) {
                lastReadAt = OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, java.time.ZoneOffset.UTC);
            }
            
            // Count unread messages - blocking prevents sending new messages, so count normally
            unreadCount = messageRepository.countUnreadMessages(conversation.getId(), currentUserId, lastReadAt);
            
            log.info("📊 [DirectChatService] getConversation - conversationId: {}, currentUserId: {}, lastReadAt: {}, unreadCount: {}", 
                    conversation.getId(), currentUserId, lastReadAt, unreadCount);
        } else {
            log.warn("⚠️ [DirectChatService] getConversation - Participant not found for conversationId: {}, currentUserId: {}", 
                    conversation.getId(), currentUserId);
        }

        // Check bidirectional blocking status
        boolean isBlockedByOther = senderBlockedRecipient.isPresent(); // Other participant blocked current user
        boolean isBlockedByMe = recipientBlockedSender.isPresent(); // Current user blocked other participant
        
        // Check if users are friends
        boolean areFriends = friendshipService.areFriends(currentUserId, otherParticipantId);
        
        // Determine if current user can send messages
        // Can send only if: status=ACTIVE, areFriends=true, not blocked by either party
        boolean canSendMessage = "ACTIVE".equals(conversation.getStatus())
                && areFriends
                && !isBlockedByOther
                && !isBlockedByMe;

        return ConversationResponse.builder()
                .id(conversation.getId())
                .participant1Id(conversation.getParticipant1Id())
                .participant2Id(conversation.getParticipant2Id())
                .participant1Name(participant1Name)
                .participant2Name(participant2Name)
                .status(conversation.getStatus())
                .createdBy(conversation.getCreatedBy())
                .lastMessage(lastMessage != null ? toDirectMessageResponse(lastMessage, accessToken) : null)
                .unreadCount(unreadCount)
                .lastReadAt(lastReadAt)
                .isBlockedByOther(isBlockedByOther)
                .isBlockedByMe(isBlockedByMe)
                .areFriends(areFriends)
                .canSendMessage(canSendMessage)
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }

    private DirectMessageResponse toDirectMessageResponse(DirectMessage message, String accessToken) {
        return toDirectMessageResponse(message, accessToken, null);
    }

    private DirectMessageResponse toDirectMessageResponse(DirectMessage message, String accessToken, DirectMessageDeletion deletion) {
        String senderName = message.getSenderId() != null 
            ? residentInfoService.getResidentName(message.getSenderId(), accessToken)
            : "System";

        // Determine deleteType
        String deleteType = null;
        if (deletion != null) {
            deleteType = deletion.getDeleteType().name();
        } else if (Boolean.TRUE.equals(message.getIsDeleted())) {
            // Legacy support: if isDeleted is true but no deletion record, assume FOR_EVERYONE
            deleteType = "FOR_EVERYONE";
        }

        DirectMessageResponse.DirectMessageResponseBuilder builder = DirectMessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .senderId(message.getSenderId())
                .senderName(senderName)
                .content(message.getContent())
                .messageType(message.getMessageType())
                .imageUrl(message.getImageUrl())
                .fileUrl(message.getFileUrl())
                .fileName(message.getFileName())
                .fileSize(message.getFileSize())
                .mimeType(message.getMimeType())
                .replyToMessageId(message.getReplyToMessageId())
                .isEdited(message.getIsEdited())
                .isDeleted(message.getIsDeleted() || deletion != null)
                .deleteType(deleteType)
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
                    String postStatus = checkPostStatus(postId, accessToken);
                    builder.postStatus(postStatus);
                }
            } catch (Exception e) {
                log.warn("Failed to parse marketplace_post data: {}", e.getMessage());
            }
        }

        // Load reply message if exists
        if (message.getReplyToMessageId() != null) {
            messageRepository.findById(message.getReplyToMessageId())
                    .ifPresent(replyMessage -> builder.replyToMessage(toDirectMessageResponse(replyMessage, accessToken)));
        }

        return builder.build();
    }

    /**
     * Check post status from marketplace service
     * Returns "ACTIVE", "SOLD", "DELETED", or null if check fails
     */
    private String checkPostStatus(String postId, String accessToken) {
        try {
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

    /**
     * Get files in a conversation with pagination
     */
    @Transactional(readOnly = true)
    public DirectChatFilePagedResponse getFiles(UUID conversationId, UUID userId, int page, int size) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }
        
        // Verify user is a participant
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found: " + conversationId));
        
        // Check if user is a participant using residentId
        if (!conversation.isParticipant(residentId)) {
            throw new RuntimeException("You are not a participant in this conversation");
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<DirectChatFile> filePage = fileRepository.findByConversationIdOrderByCreatedAtDesc(
                conversationId, pageable);
        
        List<DirectChatFileResponse> fileResponses = filePage.getContent().stream()
                .map(file -> toFileResponse(file, accessToken))
                .collect(Collectors.toList());
        
        return DirectChatFilePagedResponse.builder()
                .content(fileResponses)
                .currentPage(filePage.getNumber())
                .pageSize(filePage.getSize())
                .totalElements(filePage.getTotalElements())
                .totalPages(filePage.getTotalPages())
                .hasNext(filePage.hasNext())
                .hasPrevious(filePage.hasPrevious())
                .first(filePage.isFirst())
                .last(filePage.isLast())
                .build();
    }
    
    /**
     * Convert DirectChatFile entity to DTO
     */
    private DirectChatFileResponse toFileResponse(DirectChatFile file, String accessToken) {
        String senderName = file.getSenderId() != null
            ? residentInfoService.getResidentName(file.getSenderId(), accessToken)
            : "Unknown";
        
        return DirectChatFileResponse.builder()
                .id(file.getId())
                .conversationId(file.getConversationId())
                .messageId(file.getMessageId())
                .senderId(file.getSenderId())
                .senderName(senderName)
                .fileName(file.getFileName())
                .fileSize(file.getFileSize())
                .fileType(file.getFileType())
                .mimeType(file.getMimeType())
                .fileUrl(file.getFileUrl())
                .createdAt(file.getCreatedAt())
                .build();
    }
}

