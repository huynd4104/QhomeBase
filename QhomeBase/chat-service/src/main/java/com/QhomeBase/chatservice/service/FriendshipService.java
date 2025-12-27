package com.QhomeBase.chatservice.service;

import com.QhomeBase.chatservice.model.Conversation;
import com.QhomeBase.chatservice.model.Friendship;
import com.QhomeBase.chatservice.repository.BlockRepository;
import com.QhomeBase.chatservice.repository.ConversationRepository;
import com.QhomeBase.chatservice.repository.FriendshipRepository;
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
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final ConversationRepository conversationRepository;
    private final BlockRepository blockRepository;

    /**
     * Compare two UUIDs in the same order as PostgreSQL (byte order comparison).
     * PostgreSQL compares UUIDs byte-by-byte from first to last byte.
     * This method ensures consistent ordering with PostgreSQL's UUID comparison.
     * 
     * PostgreSQL stores UUID as 16 bytes in network byte order (big-endian).
     * Java UUID stores as MSB (64 bits) + LSB (64 bits), both in big-endian.
     * We need to compare them byte-by-byte as PostgreSQL does.
     */
    private int compareUuidAsBytes(UUID uuid1, UUID uuid2) {
        // Compare MSB first (as unsigned 64-bit integers)
        long msb1 = uuid1.getMostSignificantBits();
        long msb2 = uuid2.getMostSignificantBits();
        
        // Use unsigned comparison for MSB
        int msbCompare = Long.compareUnsigned(msb1, msb2);
        if (msbCompare != 0) {
            return msbCompare;
        }
        
        // If MSB are equal, compare LSB (as unsigned 64-bit integers)
        long lsb1 = uuid1.getLeastSignificantBits();
        long lsb2 = uuid2.getLeastSignificantBits();
        return Long.compareUnsigned(lsb1, lsb2);
    }

    /**
     * Ensure user1_id < user2_id according to PostgreSQL byte order comparison.
     * Returns array [user1Id, user2Id] where user1Id < user2Id.
     * 
     * Uses native PostgreSQL query to check UUID order, ensuring 100% compatibility.
     */
    private UUID[] orderUuidsForFriendship(UUID userId1, UUID userId2) {
        // Use native PostgreSQL query to check UUID order (most reliable)
        boolean isUser1Less = friendshipRepository.isUuid1LessThanUuid2(userId1, userId2);
        
        if (isUser1Less) {
            log.debug("Ordering UUIDs (PostgreSQL native): {} < {}", userId1, userId2);
            return new UUID[]{userId1, userId2};
        } else {
            log.debug("Ordering UUIDs (PostgreSQL native): {} < {}", userId2, userId1);
            return new UUID[]{userId2, userId1};
        }
    }

    /**
     * Create or activate friendship between two users
     */
    @Transactional
    public Friendship createOrActivateFriendship(UUID userId1, UUID userId2) {
        // Validate inputs
        if (userId1 == null || userId2 == null) {
            log.warn("Cannot create/activate friendship: one or both userIds are null (userId1: {}, userId2: {})", userId1, userId2);
            throw new RuntimeException("User IDs cannot be null");
        }
        
        // Ensure user1_id < user2_id for consistency (using PostgreSQL byte order)
        UUID[] ordered = orderUuidsForFriendship(userId1, userId2);
        UUID user1Id = ordered[0];
        UUID user2Id = ordered[1];

        return friendshipRepository.findFriendshipBetweenUsers(user1Id, user2Id)
                .map(existingFriendship -> {
                    // Friendship exists - activate it if inactive
                    if (!Boolean.TRUE.equals(existingFriendship.getIsActive())) {
                        existingFriendship.setIsActive(true);
                        existingFriendship = friendshipRepository.save(existingFriendship);
                        log.info("Activated existing friendship between {} and {}", user1Id, user2Id);
                    }
                    return existingFriendship;
                })
                .orElseGet(() -> {
                    // Create new friendship
                    // Double-check ordering before save (PostgreSQL will validate anyway)
                    log.info("Creating new friendship: user1Id={}, user2Id={}", user1Id, user2Id);
                    log.info("  UUID comparison (Java): compareTo={}, compareUuidAsBytes={}", 
                            user1Id.compareTo(user2Id), compareUuidAsBytes(user1Id, user2Id));
                    
                    Friendship friendship = Friendship.builder()
                            .user1Id(user1Id)
                            .user2Id(user2Id)
                            .isActive(true)
                            .build();
                    friendship = friendshipRepository.save(friendship);
                    log.info("Created new friendship between {} and {}", user1Id, user2Id);
                    return friendship;
                });
    }

    /**
     * Deactivate friendship (when one user blocks the other or unfriends)
     * If not blocked, conversation status becomes LOCKED (can view history, cannot send messages)
     */
    @Transactional
    public void deactivateFriendship(UUID userId1, UUID userId2) {
        // Validate inputs
        if (userId1 == null || userId2 == null) {
            log.warn("Cannot deactivate friendship: one or both userIds are null (userId1: {}, userId2: {})", userId1, userId2);
            return;
        }
        
        // Ensure user1_id < user2_id for consistency (using PostgreSQL byte order)
        UUID[] ordered = orderUuidsForFriendship(userId1, userId2);
        UUID user1Id = ordered[0];
        UUID user2Id = ordered[1];

        friendshipRepository.findFriendshipBetweenUsers(user1Id, user2Id)
                .ifPresent(friendship -> {
                    if (Boolean.TRUE.equals(friendship.getIsActive())) {
                        friendship.setIsActive(false);
                        friendshipRepository.save(friendship);
                        log.info("Deactivated friendship between {} and {}", user1Id, user2Id);
                        
                        // Update conversation status if not blocked
                        // If blocked, conversation status is already BLOCKED (handled by BlockService)
                        // If not blocked, set conversation to LOCKED (can view history, cannot send)
                        Optional<Conversation> conversationOpt = conversationRepository
                                .findConversationBetweenParticipants(user1Id, user2Id);
                        
                        if (conversationOpt.isPresent()) {
                            Conversation conversation = conversationOpt.get();
                            // Check if either user has blocked the other
                            boolean isBlocked = blockRepository.findByBlockerIdAndBlockedId(user1Id, user2Id).isPresent()
                                    || blockRepository.findByBlockerIdAndBlockedId(user2Id, user1Id).isPresent();
                            
                            if (!isBlocked && "ACTIVE".equals(conversation.getStatus())) {
                                // Not blocked but friendship deactivated -> LOCKED
                                conversation.setStatus("LOCKED");
                                conversationRepository.save(conversation);
                                log.info("Changed conversation {} status from ACTIVE to LOCKED after friendship deactivation", 
                                        conversation.getId());
                            }
                        }
                    }
                });
    }

    /**
     * Reactivate friendship if it exists (when unblocking)
     * Only reactivates existing friendship, does not create new one
     */
    @Transactional
    public void reactivateFriendshipIfExists(UUID userId1, UUID userId2) {
        // Validate inputs
        if (userId1 == null || userId2 == null) {
            log.warn("Cannot reactivate friendship: one or both userIds are null (userId1: {}, userId2: {})", userId1, userId2);
            return;
        }
        
        // Ensure user1_id < user2_id for consistency (using PostgreSQL byte order)
        UUID[] ordered = orderUuidsForFriendship(userId1, userId2);
        UUID user1Id = ordered[0];
        UUID user2Id = ordered[1];

        friendshipRepository.findFriendshipBetweenUsers(user1Id, user2Id)
                .ifPresent(friendship -> {
                    if (!Boolean.TRUE.equals(friendship.getIsActive())) {
                        friendship.setIsActive(true);
                        friendshipRepository.save(friendship);
                        log.info("Reactivated friendship between {} and {} (user unblocked)", user1Id, user2Id);
                    } else {
                        log.debug("Friendship between {} and {} is already active", user1Id, user2Id);
                    }
                });
        
        // If friendship doesn't exist, don't create it - they weren't friends before block
        // This ensures we don't create friendships for users who weren't friends originally
    }

    /**
     * Get all active friendships for a user
     */
    @Transactional(readOnly = true)
    public List<Friendship> getActiveFriendships(UUID userId) {
        return friendshipRepository.findActiveFriendshipsByUserId(userId);
    }

    /**
     * Check if two users are friends (active friendship exists)
     */
    @Transactional(readOnly = true)
    public boolean areFriends(UUID userId1, UUID userId2) {
        // Validate inputs
        if (userId1 == null || userId2 == null) {
            log.debug("Cannot check friendship: one or both userIds are null (userId1: {}, userId2: {})", userId1, userId2);
            return false;
        }
        
        // Ensure user1_id < user2_id for consistency (using PostgreSQL byte order)
        UUID[] ordered = orderUuidsForFriendship(userId1, userId2);
        UUID user1Id = ordered[0];
        UUID user2Id = ordered[1];

        return friendshipRepository.findFriendshipBetweenUsers(user1Id, user2Id)
                .map(friendship -> Boolean.TRUE.equals(friendship.getIsActive()))
                .orElse(false);
    }
}

