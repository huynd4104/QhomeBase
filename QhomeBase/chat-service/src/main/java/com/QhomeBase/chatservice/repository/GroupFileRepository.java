package com.QhomeBase.chatservice.repository;

import com.QhomeBase.chatservice.model.GroupFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GroupFileRepository extends JpaRepository<GroupFile, UUID> {

    /**
     * Find all files in a group, ordered by createdAt descending (newest first)
     */
    @Query("SELECT gf FROM GroupFile gf WHERE gf.groupId = :groupId ORDER BY gf.createdAt DESC")
    Page<GroupFile> findByGroupIdOrderByCreatedAtDesc(@Param("groupId") UUID groupId, Pageable pageable);

    /**
     * Check if a file exists for a given message
     */
    boolean existsByMessageId(UUID messageId);

    /**
     * Find file by message ID
     */
    GroupFile findByMessageId(UUID messageId);

    /**
     * Find files uploaded by a user in a group from a specific time onwards
     */
    @Query("SELECT gf FROM GroupFile gf WHERE gf.groupId = :groupId " +
           "AND gf.senderId = :senderId " +
           "AND gf.createdAt >= :fromTime")
    List<GroupFile> findFilesByGroupIdAndSenderIdFromTime(
        @Param("groupId") UUID groupId,
        @Param("senderId") UUID senderId,
        @Param("fromTime") java.time.OffsetDateTime fromTime
    );
}

