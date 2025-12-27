package com.QhomeBase.chatservice.repository;

import com.QhomeBase.chatservice.model.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {
    
    /**
     * Check UUID order in PostgreSQL (returns true if uuid1 < uuid2 in PostgreSQL)
     */
    @Query(value = "SELECT :uuid1 < :uuid2", nativeQuery = true)
    boolean isUuid1LessThanUuid2(@Param("uuid1") UUID uuid1, @Param("uuid2") UUID uuid2);

    /**
     * Find friendship between two users
     * Ensures user1_id < user2_id for consistency
     */
    @Query("SELECT f FROM Friendship f WHERE " +
           "((f.user1Id = :userId1 AND f.user2Id = :userId2) OR " +
           " (f.user1Id = :userId2 AND f.user2Id = :userId1))")
    Optional<Friendship> findFriendshipBetweenUsers(
        @Param("userId1") UUID userId1,
        @Param("userId2") UUID userId2
    );

    /**
     * Find all active friendships for a user
     */
    @Query("SELECT f FROM Friendship f WHERE " +
           "(f.user1Id = :userId OR f.user2Id = :userId) " +
           "AND f.isActive = true " +
           "ORDER BY f.updatedAt DESC")
    List<Friendship> findActiveFriendshipsByUserId(@Param("userId") UUID userId);

    /**
     * Find all friendships (including inactive) for a user
     */
    @Query("SELECT f FROM Friendship f WHERE " +
           "(f.user1Id = :userId OR f.user2Id = :userId) " +
           "ORDER BY f.updatedAt DESC")
    List<Friendship> findAllFriendshipsByUserId(@Param("userId") UUID userId);
}

