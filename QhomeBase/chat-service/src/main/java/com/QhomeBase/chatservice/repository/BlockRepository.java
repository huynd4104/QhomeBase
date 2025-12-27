package com.QhomeBase.chatservice.repository;

import com.QhomeBase.chatservice.model.Block;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BlockRepository extends JpaRepository<Block, UUID> {

    /**
     * Check if user1 has blocked user2
     */
    Optional<Block> findByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);

    /**
     * Check if two users have blocked each other (bidirectional check)
     */
    @Query("SELECT b FROM Block b WHERE " +
           "(b.blockerId = :userId1 AND b.blockedId = :userId2) OR " +
           "(b.blockerId = :userId2 AND b.blockedId = :userId1)")
    List<Block> findBlocksBetweenUsers(
        @Param("userId1") UUID userId1,
        @Param("userId2") UUID userId2
    );

    /**
     * Find all users blocked by a user
     */
    List<Block> findByBlockerId(UUID blockerId);

    /**
     * Find all users who blocked a user
     */
    List<Block> findByBlockedId(UUID blockedId);
}

