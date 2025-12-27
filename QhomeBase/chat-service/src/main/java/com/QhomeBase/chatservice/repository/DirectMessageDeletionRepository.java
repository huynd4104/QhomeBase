package com.QhomeBase.chatservice.repository;

import com.QhomeBase.chatservice.model.DirectMessageDeletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DirectMessageDeletionRepository extends JpaRepository<DirectMessageDeletion, UUID> {

    /**
     * Find all deletions for a message
     */
    List<DirectMessageDeletion> findByMessageId(UUID messageId);

    /**
     * Find deletion by message and user
     */
    @Query("SELECT d FROM DirectMessageDeletion d WHERE d.messageId = :messageId AND d.deletedByUserId = :userId")
    List<DirectMessageDeletion> findByMessageIdAndDeletedByUserId(
        @Param("messageId") UUID messageId,
        @Param("userId") UUID userId
    );

    /**
     * Check if message is deleted for everyone
     */
    @Query("SELECT COUNT(d) > 0 FROM DirectMessageDeletion d WHERE d.messageId = :messageId AND d.deleteType = 'FOR_EVERYONE'")
    boolean isDeletedForEveryone(@Param("messageId") UUID messageId);

    /**
     * Check if message is deleted for a specific user
     */
    @Query("SELECT COUNT(d) > 0 FROM DirectMessageDeletion d WHERE d.messageId = :messageId AND d.deletedByUserId = :userId")
    boolean isDeletedForUser(@Param("messageId") UUID messageId, @Param("userId") UUID userId);

    /**
     * Delete all deletions for a message (when message is permanently deleted)
     */
    void deleteByMessageId(UUID messageId);

    /**
     * Find all deletions for multiple messages
     */
    @Query("SELECT d FROM DirectMessageDeletion d WHERE d.messageId IN :messageIds")
    List<DirectMessageDeletion> findByMessageIdIn(@Param("messageIds") List<UUID> messageIds);
}

