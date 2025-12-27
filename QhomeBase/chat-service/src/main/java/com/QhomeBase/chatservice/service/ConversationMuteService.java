package com.QhomeBase.chatservice.service;

import com.QhomeBase.chatservice.model.ConversationParticipant;
import com.QhomeBase.chatservice.model.GroupMember;
import com.QhomeBase.chatservice.repository.ConversationParticipantRepository;
import com.QhomeBase.chatservice.repository.GroupMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationMuteService {

    private final GroupMemberRepository groupMemberRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final ResidentInfoService residentInfoService;

    /**
     * Mute a group chat
     * @param groupId Group ID
     * @param userId User ID (from JWT)
     * @param durationHours Duration in hours (null = mute indefinitely)
     * @return true if successful
     */
    @Transactional
    public boolean muteGroupChat(UUID groupId, UUID userId, Integer durationHours) {
        try {
            String accessToken = getCurrentAccessToken();
            UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
            if (residentId == null) {
                throw new RuntimeException("Resident not found for user: " + userId);
            }

            GroupMember member = groupMemberRepository.findByGroupIdAndResidentId(groupId, residentId)
                    .orElseThrow(() -> new RuntimeException("You are not a member of this group"));

            OffsetDateTime muteUntil = null;
            if (durationHours != null) {
                muteUntil = OffsetDateTime.now().plusHours(durationHours);
            }

            member.setIsMuted(true);
            member.setMuteUntil(muteUntil);
            member.setMutedByUserId(userId);
            groupMemberRepository.save(member);

            log.info("Group {} muted for resident {} until {}", groupId, residentId, muteUntil);
            return true;
        } catch (Exception e) {
            log.error("Error muting group chat: {}", e.getMessage(), e);
            throw new RuntimeException("Error muting group chat: " + e.getMessage());
        }
    }

    /**
     * Unmute a group chat
     */
    @Transactional
    public boolean unmuteGroupChat(UUID groupId, UUID userId) {
        try {
            String accessToken = getCurrentAccessToken();
            UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
            if (residentId == null) {
                throw new RuntimeException("Resident not found for user: " + userId);
            }

            GroupMember member = groupMemberRepository.findByGroupIdAndResidentId(groupId, residentId)
                    .orElseThrow(() -> new RuntimeException("You are not a member of this group"));

            member.setIsMuted(false);
            member.setMuteUntil(null);
            member.setMutedByUserId(null);
            groupMemberRepository.save(member);

            log.info("Group {} unmuted for resident {}", groupId, residentId);
            return true;
        } catch (Exception e) {
            log.error("Error unmuting group chat: {}", e.getMessage(), e);
            throw new RuntimeException("Error unmuting group chat: " + e.getMessage());
        }
    }

    /**
     * Mute a direct conversation
     */
    @Transactional
    public boolean muteDirectConversation(UUID conversationId, UUID userId, Integer durationHours) {
        try {
            String accessToken = getCurrentAccessToken();
            UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
            if (residentId == null) {
                throw new RuntimeException("Resident not found for user: " + userId);
            }

            ConversationParticipant participant = conversationParticipantRepository
                    .findByConversationIdAndResidentId(conversationId, residentId)
                    .orElseThrow(() -> new RuntimeException("You are not a participant of this conversation"));

            OffsetDateTime muteUntil = null;
            if (durationHours != null) {
                muteUntil = OffsetDateTime.now().plusHours(durationHours);
            }

            participant.setIsMuted(true);
            participant.setMuteUntil(muteUntil);
            participant.setMutedByUserId(userId);
            conversationParticipantRepository.save(participant);

            log.info("Conversation {} muted for resident {} until {}", conversationId, residentId, muteUntil);
            return true;
        } catch (Exception e) {
            log.error("Error muting direct conversation: {}", e.getMessage(), e);
            throw new RuntimeException("Error muting direct conversation: " + e.getMessage());
        }
    }

    /**
     * Unmute a direct conversation
     */
    @Transactional
    public boolean unmuteDirectConversation(UUID conversationId, UUID userId) {
        try {
            String accessToken = getCurrentAccessToken();
            UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
            if (residentId == null) {
                throw new RuntimeException("Resident not found for user: " + userId);
            }

            ConversationParticipant participant = conversationParticipantRepository
                    .findByConversationIdAndResidentId(conversationId, residentId)
                    .orElseThrow(() -> new RuntimeException("You are not a participant of this conversation"));

            participant.setIsMuted(false);
            participant.setMuteUntil(null);
            participant.setMutedByUserId(null);
            conversationParticipantRepository.save(participant);

            log.info("Conversation {} unmuted for resident {}", conversationId, residentId);
            return true;
        } catch (Exception e) {
            log.error("Error unmuting direct conversation: {}", e.getMessage(), e);
            throw new RuntimeException("Error unmuting direct conversation: " + e.getMessage());
        }
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

