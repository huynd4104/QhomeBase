package com.QhomeBase.chatservice.repository;

import com.QhomeBase.chatservice.model.DirectChatFile;
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
public interface DirectChatFileRepository extends JpaRepository<DirectChatFile, UUID> {

    /**
     * Find all files in a conversation with pagination
     */
    @Query("SELECT f FROM DirectChatFile f WHERE f.conversationId = :conversationId " +
           "ORDER BY f.createdAt DESC")
    Page<DirectChatFile> findByConversationIdOrderByCreatedAtDesc(
        @Param("conversationId") UUID conversationId,
        Pageable pageable
    );

    /**
     * Find files by type (IMAGE, DOCUMENT, etc.)
     */
    @Query("SELECT f FROM DirectChatFile f WHERE f.conversationId = :conversationId " +
           "AND f.fileType = :fileType " +
           "ORDER BY f.createdAt DESC")
    Page<DirectChatFile> findByConversationIdAndFileTypeOrderByCreatedAtDesc(
        @Param("conversationId") UUID conversationId,
        @Param("fileType") String fileType,
        Pageable pageable
    );

    /**
     * Find files uploaded by a user in a conversation from a specific time onwards
     */
    @Query("SELECT f FROM DirectChatFile f WHERE f.conversationId = :conversationId " +
           "AND f.senderId = :senderId " +
           "AND f.createdAt >= :fromTime")
    List<DirectChatFile> findFilesByConversationIdAndSenderIdFromTime(
        @Param("conversationId") UUID conversationId,
        @Param("senderId") UUID senderId,
        @Param("fromTime") OffsetDateTime fromTime
    );
}

