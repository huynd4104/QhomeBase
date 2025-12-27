package com.QhomeBase.chatservice.service;

import com.QhomeBase.chatservice.model.Conversation;
import com.QhomeBase.chatservice.model.ConversationParticipant;
import com.QhomeBase.chatservice.model.DirectChatFile;
import com.QhomeBase.chatservice.model.DirectMessage;
import com.QhomeBase.chatservice.repository.ConversationParticipantRepository;
import com.QhomeBase.chatservice.repository.ConversationRepository;
import com.QhomeBase.chatservice.repository.DirectChatFileRepository;
import com.QhomeBase.chatservice.repository.DirectMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationHideService {

    private final ConversationParticipantRepository conversationParticipantRepository;
    private final ConversationRepository conversationRepository;
    private final DirectMessageRepository directMessageRepository;
    private final DirectChatFileRepository directChatFileRepository;
    private final ResidentInfoService residentInfoService;

    /**
     * Hide a direct conversation (client-side only)
     * This will reset unreadCount to 0 and mark conversation as hidden
     */
    @Transactional
    public boolean hideDirectConversation(UUID conversationId, UUID userId) {
        try {
            String accessToken = getCurrentAccessToken();
            UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
            if (residentId == null) {
                throw new RuntimeException("Resident not found for user: " + userId);
            }

            ConversationParticipant participant = conversationParticipantRepository
                    .findByConversationIdAndResidentId(conversationId, residentId)
                    .orElseThrow(() -> new RuntimeException("You are not a participant of this conversation"));

            OffsetDateTime deleteTime = OffsetDateTime.now();

            // Mark as hidden (set hiddenAt)
            participant.setIsHidden(true);
            participant.setHiddenAt(deleteTime);
            
            // Reset lastReadAt to null so that when a new message arrives,
            // it will be considered as the first message (like Messenger)
            participant.setLastReadAt(null);
            
            conversationParticipantRepository.save(participant);

            // Delete all messages sent by this user from the deletion time onwards
            deleteUserMessagesFromConversation(conversationId, residentId, deleteTime);

            // Delete all files/images uploaded by this user from the deletion time onwards
            deleteUserFilesFromConversation(conversationId, residentId, deleteTime);

            log.info("User {} hid conversation {} and deleted their messages/files from {}", residentId, conversationId, deleteTime);

            // Check if both participants have hidden the conversation
            Conversation conversation = conversationRepository.findById(conversationId)
                    .orElse(null);
            if (conversation != null) {
                List<ConversationParticipant> allParticipants = conversationParticipantRepository
                        .findByConversationId(conversationId);
                
                boolean bothHidden = allParticipants.stream()
                        .allMatch(p -> Boolean.TRUE.equals(p.getIsHidden()));
                
                if (bothHidden && allParticipants.size() == 2) {
                    // Both participants have hidden the conversation - mark as DELETED
                    conversation.setStatus("DELETED");
                    conversationRepository.save(conversation);
                    log.info("Conversation {} marked as DELETED (both participants have hidden it)", conversationId);
                }
            }

            log.info("Conversation {} hidden for resident {}", conversationId, residentId);
            return true;
        } catch (Exception e) {
            log.error("Error hiding direct conversation: {}", e.getMessage(), e);
            throw new RuntimeException("Error hiding direct conversation: " + e.getMessage());
        }
    }

    /**
     * Unhide a direct conversation (when new message arrives)
     * This is called automatically when a new message is sent to a hidden conversation
     */
    @Transactional
    public void unhideDirectConversation(UUID conversationId, UUID residentId) {
        try {
            ConversationParticipant participant = conversationParticipantRepository
                    .findByConversationIdAndResidentId(conversationId, residentId)
                    .orElse(null);
            
            if (participant != null && Boolean.TRUE.equals(participant.getIsHidden())) {
                participant.setIsHidden(false);
                participant.setHiddenAt(null);
                // Reset lastReadAt to null so the new message is considered as the first message
                participant.setLastReadAt(null);
                conversationParticipantRepository.save(participant);
                log.info("Conversation {} unhidden for resident {} (new message received). lastReadAt reset to null.", conversationId, residentId);
            }
        } catch (Exception e) {
            log.error("Error unhiding direct conversation: {}", e.getMessage(), e);
        }
    }

    /**
     * Delete all messages sent by a user in a conversation from a specific time onwards
     */
    @Transactional
    private void deleteUserMessagesFromConversation(UUID conversationId, UUID senderId, OffsetDateTime fromTime) {
        List<DirectMessage> messages = directMessageRepository.findMessagesByConversationIdAndSenderIdFromTime(
            conversationId, senderId, fromTime
        );
        
        for (DirectMessage message : messages) {
            message.setIsDeleted(true);
            message.setContent(null); // Clear content for deleted messages
            // Keep file/image URLs for now, but mark as deleted
        }
        
        directMessageRepository.saveAll(messages);
        log.info("Deleted {} messages from user {} in conversation {} from {}", 
                messages.size(), senderId, conversationId, fromTime);
    }

    /**
     * Delete all files/images uploaded by a user in a conversation from a specific time onwards
     */
    @Transactional
    private void deleteUserFilesFromConversation(UUID conversationId, UUID senderId, OffsetDateTime fromTime) {
        List<DirectChatFile> files = directChatFileRepository.findFilesByConversationIdAndSenderIdFromTime(
            conversationId, senderId, fromTime
        );
        
        // Delete the DirectChatFile records (soft delete - actual files can be cleaned up separately)
        directChatFileRepository.deleteAll(files);
        
        log.info("Deleted {} files from user {} in conversation {} from {}", 
                files.size(), senderId, conversationId, fromTime);
    }

    private String getCurrentAccessToken() {
        try {
            org.springframework.security.core.Authentication authentication = 
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof com.QhomeBase.chatservice.security.UserPrincipal) {
                com.QhomeBase.chatservice.security.UserPrincipal principal = 
                        (com.QhomeBase.chatservice.security.UserPrincipal) authentication.getPrincipal();
                return principal.token();
            }
        } catch (Exception e) {
            log.debug("Could not get token from SecurityContext: {}", e.getMessage());
        }
        return null;
    }
}

