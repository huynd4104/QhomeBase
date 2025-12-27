package com.QhomeBase.chatservice.repository;

import com.QhomeBase.chatservice.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    /**
     * Find conversation between two participants
     * Ensures participant1_id < participant2_id for consistency
     */
    @Query("SELECT c FROM Conversation c WHERE " +
           "((c.participant1Id = :userId1 AND c.participant2Id = :userId2) OR " +
           " (c.participant1Id = :userId2 AND c.participant2Id = :userId1))")
    Optional<Conversation> findConversationBetweenParticipants(
        @Param("userId1") UUID userId1,
        @Param("userId2") UUID userId2
    );

    /**
     * Find all active conversations for a user
     */
    @Query("SELECT c FROM Conversation c WHERE " +
           "(c.participant1Id = :userId OR c.participant2Id = :userId) " +
           "AND c.status = 'ACTIVE' " +
           "ORDER BY c.updatedAt DESC")
    List<Conversation> findActiveConversationsByUserId(@Param("userId") UUID userId);

    /**
     * Find all conversations for a user (ACTIVE, BLOCKED, LOCKED, PENDING - but not DELETED)
     * This includes blocked and locked conversations so users can see all their conversations
     */
    @Query("SELECT c FROM Conversation c WHERE " +
           "(c.participant1Id = :userId OR c.participant2Id = :userId) " +
           "AND c.status IN ('ACTIVE', 'BLOCKED', 'LOCKED', 'PENDING') " +
           "ORDER BY c.updatedAt DESC")
    List<Conversation> findVisibleConversationsByUserId(@Param("userId") UUID userId);

    /**
     * Find all conversations (including pending) for a user
     */
    @Query("SELECT c FROM Conversation c WHERE " +
           "(c.participant1Id = :userId OR c.participant2Id = :userId) " +
           "ORDER BY c.updatedAt DESC")
    List<Conversation> findAllConversationsByUserId(@Param("userId") UUID userId);
}

