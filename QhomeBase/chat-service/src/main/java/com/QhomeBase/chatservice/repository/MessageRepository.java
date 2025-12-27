package com.QhomeBase.chatservice.repository;

import com.QhomeBase.chatservice.model.Message;
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
public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Query("SELECT m FROM Message m WHERE m.groupId = :groupId AND m.isDeleted = false ORDER BY m.createdAt DESC")
    Page<Message> findMessagesByGroupIdOrderByCreatedAtDesc(
            @Param("groupId") UUID groupId, 
            Pageable pageable
    );

    @Query("SELECT m FROM Message m WHERE m.groupId = :groupId AND m.createdAt > :after AND m.messageType != 'SYSTEM' AND m.isDeleted = false ORDER BY m.createdAt ASC")
    List<Message> findNewMessagesByGroupIdAfter(
            @Param("groupId") UUID groupId,
            @Param("after") OffsetDateTime after
    );

    @Query("SELECT m FROM Message m WHERE m.groupId = :groupId AND m.createdAt > :after AND m.senderId != :excludeSenderId AND m.messageType != 'SYSTEM' AND m.isDeleted = false ORDER BY m.createdAt ASC")
    List<Message> findNewMessagesByGroupIdAfterExcludingSender(
            @Param("groupId") UUID groupId,
            @Param("after") OffsetDateTime after,
            @Param("excludeSenderId") UUID excludeSenderId
    );

    @Query("SELECT COUNT(m) FROM Message m WHERE m.groupId = :groupId AND m.isDeleted = false")
    Long countByGroupId(@Param("groupId") UUID groupId);

    @Query("SELECT m FROM Message m WHERE m.id = :messageId AND m.isDeleted = false")
    Message findByIdAndNotDeleted(@Param("messageId") UUID messageId);

    /**
     * Count unread messages for a user in a group
     */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.groupId = :groupId " +
           "AND m.senderId != :excludeSenderId " +
           "AND m.createdAt > :lastReadAt " +
           "AND m.isDeleted = false " +
           "AND m.messageType != 'SYSTEM'")
    Long countUnreadMessages(
        @Param("groupId") UUID groupId,
        @Param("excludeSenderId") UUID excludeSenderId,
        @Param("lastReadAt") OffsetDateTime lastReadAt
    );

    /**
     * Find messages sent by a user in a group from a specific time onwards
     */
    @Query("SELECT m FROM Message m WHERE m.groupId = :groupId " +
           "AND m.senderId = :senderId " +
           "AND m.createdAt >= :fromTime " +
           "AND m.isDeleted = false")
    List<Message> findMessagesByGroupIdAndSenderIdFromTime(
        @Param("groupId") UUID groupId,
        @Param("senderId") UUID senderId,
        @Param("fromTime") OffsetDateTime fromTime
    );
}

