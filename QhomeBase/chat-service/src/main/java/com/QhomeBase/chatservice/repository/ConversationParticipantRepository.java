package com.QhomeBase.chatservice.repository;

import com.QhomeBase.chatservice.model.ConversationParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, UUID> {

    /**
     * Find participant in a conversation
     */
    Optional<ConversationParticipant> findByConversationIdAndResidentId(
        UUID conversationId,
        UUID residentId
    );

    /**
     * Find all participants in a conversation
     */
    List<ConversationParticipant> findByConversationId(UUID conversationId);

    /**
     * Find all conversations for a user
     */
    @Query("SELECT cp FROM ConversationParticipant cp WHERE cp.residentId = :residentId")
    List<ConversationParticipant> findByResidentId(@Param("residentId") UUID residentId);
}

