package com.QhomeBase.chatservice.service;

import com.QhomeBase.chatservice.model.Block;
import com.QhomeBase.chatservice.model.Conversation;
import com.QhomeBase.chatservice.model.DirectInvitation;
import com.QhomeBase.chatservice.model.GroupInvitation;
import com.QhomeBase.chatservice.repository.BlockRepository;
import com.QhomeBase.chatservice.repository.ConversationRepository;
import com.QhomeBase.chatservice.repository.DirectInvitationRepository;
import com.QhomeBase.chatservice.repository.GroupInvitationRepository;
import com.QhomeBase.chatservice.repository.GroupMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlockService {

    private final BlockRepository blockRepository;
    private final FriendshipService friendshipService;
    private final DirectInvitationRepository invitationRepository;
    private final ConversationRepository conversationRepository;
    private final GroupInvitationRepository groupInvitationRepository;
    private final GroupMemberRepository groupMemberRepository;

    /**
     * Block a user
     */
    @Transactional
    public void blockUser(UUID blockerId, UUID blockedId) {
        if (blockerId == null || blockedId == null) {
            throw new RuntimeException("Blocker ID and blocked ID cannot be null");
        }
        
        if (blockerId.equals(blockedId)) {
            throw new RuntimeException("Cannot block yourself");
        }

        // Check if already blocked
        if (blockRepository.findByBlockerIdAndBlockedId(blockerId, blockedId).isPresent()) {
            throw new RuntimeException("User is already blocked");
        }

        // Create block record
        Block block = Block.builder()
                .blockerId(blockerId)
                .blockedId(blockedId)
                .build();
        blockRepository.save(block);

        // Change conversation status from ACTIVE to BLOCKED
        // After block, users cannot chat until unblock + invitation + accept
        UUID participant1Id = blockerId.compareTo(blockedId) < 0 ? blockerId : blockedId;
        UUID participant2Id = blockerId.compareTo(blockedId) < 0 ? blockedId : blockerId;
        
        Optional<Conversation> conversationOpt = conversationRepository
                .findConversationBetweenParticipants(participant1Id, participant2Id);
        
        if (conversationOpt.isPresent()) {
            Conversation conversation = conversationOpt.get();
            if ("ACTIVE".equals(conversation.getStatus())) {
                conversation.setStatus("BLOCKED");
                conversationRepository.save(conversation);
                log.info("Changed conversation {} status from ACTIVE to BLOCKED after block", conversation.getId());
            }
        }

        // Deactivate friendship if exists
        friendshipService.deactivateFriendship(blockerId, blockedId);

        // Delete PENDING direct invitations from blocker to blocked user
        // When A blocks B, any PENDING invitation from A to B should be deleted
        // B will not see the invitation anymore, and A needs to re-invite after unblock
        List<DirectInvitation> directInvitations = invitationRepository.findInvitationsBetweenUsers(blockerId, blockedId);
        for (DirectInvitation inv : directInvitations) {
            // Only delete invitations from blocker to blocked (A -> B)
            // Also delete reverse invitations (B -> A) since they can't chat anyway
            if ("PENDING".equals(inv.getStatus())) {
                invitationRepository.delete(inv);
                log.info("Deleted PENDING direct invitation {} after block ({} -> {}). Status was: {}", 
                        inv.getId(), inv.getInviterId(), inv.getInviteeId(), inv.getStatus());
            }
        }

        // Delete PENDING group invitations from blocker to blocked user
        // When A blocks B, any PENDING group invitation from A to B should be deleted
        // But only if B is not already a member of that group
        List<GroupInvitation> groupInvitations = groupInvitationRepository.findPendingInvitationsFromInviterToInvitee(blockerId, blockedId);
        for (GroupInvitation inv : groupInvitations) {
            // Check if blocked user is already a member of the group
            boolean isMember = groupMemberRepository.existsByGroupIdAndResidentId(inv.getGroupId(), blockedId);
            
            if (!isMember) {
                // B is not a member yet - delete the invitation
                groupInvitationRepository.delete(inv);
                log.info("Deleted PENDING group invitation {} after block (Inviter: {}, GroupId: {}, Invitee: {}). B is not a member yet.", 
                        inv.getId(), inv.getInviterId(), inv.getGroupId(), inv.getInviteeResidentId());
            } else {
                // B is already a member - don't delete invitation, just log
                log.info("Skipped deleting group invitation {} after block (Inviter: {}, GroupId: {}, Invitee: {}). B is already a member of the group.", 
                        inv.getId(), inv.getInviterId(), inv.getGroupId(), inv.getInviteeResidentId());
            }
        }

        log.info("User {} blocked user {}. Conversation status changed to BLOCKED. Friendship deactivated. PENDING invitations deleted.", blockerId, blockedId);
    }

    /**
     * Unblock a user
     * This method is idempotent - if the user is not blocked, it will return successfully without error
     */
    @Transactional
    public void unblockUser(UUID blockerId, UUID blockedId) {
        if (blockerId == null || blockedId == null) {
            throw new RuntimeException("Blocker ID and blocked ID cannot be null");
        }
        
        // Check if user is actually blocked
        var blockOpt = blockRepository.findByBlockerIdAndBlockedId(blockerId, blockedId);
        
        if (blockOpt.isEmpty()) {
            // User is not blocked - this is fine, just return (idempotent operation)
            log.debug("User {} attempted to unblock user {}, but user was not blocked. Operation completed successfully (idempotent).", blockerId, blockedId);
            return;
        }

        Block block = blockOpt.get();
        blockRepository.delete(block);

        // Change conversation status from BLOCKED to PENDING
        // After unblock, users cannot chat until invitation is sent and accepted
        UUID participant1Id = blockerId.compareTo(blockedId) < 0 ? blockerId : blockedId;
        UUID participant2Id = blockerId.compareTo(blockedId) < 0 ? blockedId : blockerId;
        
        Optional<Conversation> conversationOpt = conversationRepository
                .findConversationBetweenParticipants(participant1Id, participant2Id);
        
        if (conversationOpt.isPresent()) {
            Conversation conversation = conversationOpt.get();
            if ("BLOCKED".equals(conversation.getStatus()) || "ACTIVE".equals(conversation.getStatus())) {
                // After unblock, users are no longer friends, so conversation becomes LOCKED
                // They can view history but cannot send messages until invitation is sent and accepted
                conversation.setStatus("LOCKED");
                conversationRepository.save(conversation);
                log.info("Changed conversation {} status from {} to LOCKED after unblock", 
                        conversation.getId(), conversation.getStatus());
            }
        }

        // Do NOT reactivate friendship - users are no longer friends after block/unblock
        // They need to send invitation again to become friends and chat directly
        // Friendship remains inactive/deleted - users must re-establish friendship through invitation
        
        // Cancel/delete all invitations between the two users (bidirectional)
        // After unblock, all invitations are cancelled - users are in "chưa gửi lời mời" state
        // They need to send a new invitation to start chatting again
        List<DirectInvitation> invitations = invitationRepository.findInvitationsBetweenUsers(blockerId, blockedId);
        
        for (DirectInvitation inv : invitations) {
            // Delete all invitations (PENDING, ACCEPTED, DECLINED) to reset to "chưa gửi lời mời" state
            invitationRepository.delete(inv);
            log.info("Deleted invitation {} after unblock ({} -> {}). Status was: {}", 
                    inv.getId(), inv.getInviterId(), inv.getInviteeId(), inv.getStatus());
        }

        log.info("User {} unblocked user {}. Conversation status changed to PENDING. Friendship remains inactive - all invitations deleted. Users are now in 'chưa gửi lời mời' state and need to send new invitation.", blockerId, blockedId);
    }

    /**
     * Check if user1 has blocked user2
     */
    public boolean isBlocked(UUID blockerId, UUID blockedId) {
        return blockRepository.findByBlockerIdAndBlockedId(blockerId, blockedId).isPresent();
    }

    /**
     * Check if two users have blocked each other (bidirectional)
     */
    public boolean areBlocked(UUID userId1, UUID userId2) {
        List<Block> blocks = blockRepository.findBlocksBetweenUsers(userId1, userId2);
        return !blocks.isEmpty();
    }

    /**
     * Get all blocked users for a user
     */
    public List<UUID> getBlockedUserIds(UUID userId) {
        return blockRepository.findByBlockerId(userId).stream()
                .map(Block::getBlockedId)
                .toList();
    }
}

