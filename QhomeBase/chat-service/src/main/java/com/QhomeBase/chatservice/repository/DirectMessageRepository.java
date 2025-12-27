package com.QhomeBase.chatservice.repository;

import com.QhomeBase.chatservice.model.DirectMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface DirectMessageRepository extends JpaRepository<DirectMessage, UUID> {

    /**
     * Find messages in a conversation with pagination
     * Ordered by created_at DESC (newest first)
     * Includes deleted messages (they will show as "Đã xóa")
     */
    @Query("SELECT m FROM DirectMessage m WHERE m.conversationId = :conversationId " +
           "ORDER BY m.createdAt DESC")
    Page<DirectMessage> findByConversationIdOrderByCreatedAtDesc(
        @Param("conversationId") UUID conversationId,
        Pageable pageable
    );

    /**
     * Find messages after a specific timestamp (for real-time updates)
     */
    @Query("SELECT m FROM DirectMessage m WHERE m.conversationId = :conversationId " +
           "AND m.createdAt > :afterTimestamp " +
           "AND m.isDeleted = false " +
           "AND m.messageType != 'SYSTEM' " +
           "ORDER BY m.createdAt ASC")
    List<DirectMessage> findNewMessagesByConversationIdAfter(
        @Param("conversationId") UUID conversationId,
        @Param("afterTimestamp") OffsetDateTime afterTimestamp
    );

    /**
     * Count unread messages for a user in a conversation
     */
    @Query("SELECT COUNT(m) FROM DirectMessage m WHERE m.conversationId = :conversationId " +
           "AND m.senderId != :userId " +
           "AND m.createdAt > :lastReadAt " +
           "AND m.isDeleted = false " +
           "AND m.messageType != 'SYSTEM'")
    Long countUnreadMessages(
        @Param("conversationId") UUID conversationId,
        @Param("userId") UUID userId,
        @Param("lastReadAt") OffsetDateTime lastReadAt
    );

    /**
     * Find messages after a specific timestamp ordered by created_at DESC
     * Used for filtering unread messages
     */
    @Query("SELECT m FROM DirectMessage m WHERE m.conversationId = :conversationId " +
           "AND m.createdAt > :afterTimestamp " +
           "ORDER BY m.createdAt DESC")
    Page<DirectMessage> findByConversationIdAndCreatedAtAfterOrderByCreatedAtDesc(
        @Param("conversationId") UUID conversationId,
        @Param("afterTimestamp") OffsetDateTime afterTimestamp,
        Pageable pageable
    );

    /**
     * Find messages sent by a user in a conversation from a specific time onwards
     */
    @Query("SELECT m FROM DirectMessage m WHERE m.conversationId = :conversationId " +
           "AND m.senderId = :senderId " +
           "AND m.createdAt >= :fromTime " +
           "AND m.isDeleted = false")
    List<DirectMessage> findMessagesByConversationIdAndSenderIdFromTime(
        @Param("conversationId") UUID conversationId,
        @Param("senderId") UUID senderId,
        @Param("fromTime") OffsetDateTime fromTime
    );
}

