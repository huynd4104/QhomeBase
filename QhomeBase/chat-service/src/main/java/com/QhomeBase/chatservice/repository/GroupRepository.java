package com.QhomeBase.chatservice.repository;

import com.QhomeBase.chatservice.model.Group;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupRepository extends JpaRepository<Group, UUID> {

    @Query("SELECT g FROM Group g WHERE g.id IN " +
           "(SELECT gm.group.id FROM GroupMember gm WHERE gm.residentId = :residentId AND gm.hiddenAt IS NULL) " +
           "AND g.isActive = true " +
           "ORDER BY g.updatedAt DESC")
    Page<Group> findGroupsByResidentId(@Param("residentId") UUID residentId, Pageable pageable);

    @Query("SELECT g FROM Group g WHERE g.buildingId = :buildingId AND g.isActive = true ORDER BY g.createdAt DESC")
    Page<Group> findGroupsByBuildingId(@Param("buildingId") UUID buildingId, Pageable pageable);

    @Query("SELECT g FROM Group g WHERE g.id = :groupId AND g.isActive = true")
    Optional<Group> findActiveGroupById(@Param("groupId") UUID groupId);

    @Query("SELECT COUNT(gm) FROM GroupMember gm WHERE gm.groupId = :groupId")
    Long countMembersByGroupId(@Param("groupId") UUID groupId);
}

