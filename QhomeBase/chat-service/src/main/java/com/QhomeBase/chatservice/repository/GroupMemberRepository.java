package com.QhomeBase.chatservice.repository;

import com.QhomeBase.chatservice.model.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, UUID> {

    Optional<GroupMember> findByGroupIdAndResidentId(UUID groupId, UUID residentId);

    List<GroupMember> findByGroupId(UUID groupId);

    List<GroupMember> findByResidentId(UUID residentId);

    @Query("SELECT gm FROM GroupMember gm WHERE gm.groupId = :groupId AND gm.role = 'ADMIN'")
    List<GroupMember> findAdminsByGroupId(@Param("groupId") UUID groupId);

    @Query("SELECT COUNT(gm) FROM GroupMember gm WHERE gm.groupId = :groupId")
    Long countByGroupId(@Param("groupId") UUID groupId);

    boolean existsByGroupIdAndResidentId(UUID groupId, UUID residentId);

    /**
     * Find all members of a group who have hidden it (hiddenAt IS NOT NULL)
     */
    @Query("SELECT gm FROM GroupMember gm WHERE gm.groupId = :groupId AND gm.hiddenAt IS NOT NULL")
    List<GroupMember> findByGroupIdAndHiddenAtIsNotNull(@Param("groupId") UUID groupId);
}

