package com.QhomeBase.chatservice.repository;

import com.QhomeBase.chatservice.model.DirectInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DirectInvitationRepository extends JpaRepository<DirectInvitation, UUID> {

    /**
     * Find pending invitation for a specific conversation
     */
    Optional<DirectInvitation> findByConversationIdAndStatus(
        UUID conversationId,
        String status
    );

    /**
     * Find pending invitations for a user (as invitee)
     * Invitations no longer expire - they remain until accepted or declined
     */
    @Query("SELECT i FROM DirectInvitation i WHERE i.inviteeId = :userId " +
           "AND i.status = 'PENDING' " +
           "ORDER BY i.createdAt DESC")
    List<DirectInvitation> findPendingInvitationsByInviteeId(@Param("userId") UUID userId);

    /**
     * Count pending invitations for a user
     * Invitations no longer expire - they remain until accepted or declined
     */
    @Query("SELECT COUNT(i) FROM DirectInvitation i WHERE i.inviteeId = :userId " +
           "AND i.status = 'PENDING'")
    Long countPendingInvitationsByInviteeId(@Param("userId") UUID userId);

    /**
     * Find invitation by conversation and participants
     */
    @Query("SELECT i FROM DirectInvitation i WHERE i.conversationId = :conversationId " +
           "AND i.inviterId = :inviterId AND i.inviteeId = :inviteeId")
    Optional<DirectInvitation> findByConversationAndParticipants(
        @Param("conversationId") UUID conversationId,
        @Param("inviterId") UUID inviterId,
        @Param("inviteeId") UUID inviteeId
    );
    
    /**
     * Find invitations between two users (bidirectional)
     */
    @Query("SELECT i FROM DirectInvitation i WHERE " +
           "((i.inviterId = :userId1 AND i.inviteeId = :userId2) OR " +
           "(i.inviterId = :userId2 AND i.inviteeId = :userId1)) " +
           "ORDER BY i.createdAt DESC")
    List<DirectInvitation> findInvitationsBetweenUsers(
        @Param("userId1") UUID userId1,
        @Param("userId2") UUID userId2
    );
}

