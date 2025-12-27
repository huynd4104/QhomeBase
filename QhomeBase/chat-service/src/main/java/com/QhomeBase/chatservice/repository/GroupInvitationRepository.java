package com.QhomeBase.chatservice.repository;

import com.QhomeBase.chatservice.model.GroupInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupInvitationRepository extends JpaRepository<GroupInvitation, UUID> {

    @Query("SELECT gi FROM GroupInvitation gi WHERE gi.inviteePhone = :phone AND gi.status = 'PENDING'")
    List<GroupInvitation> findPendingInvitationsByPhone(@Param("phone") String phone);

    @Query("SELECT gi FROM GroupInvitation gi WHERE gi.inviteeResidentId = :residentId AND gi.status = 'PENDING'")
    List<GroupInvitation> findPendingInvitationsByResidentId(@Param("residentId") UUID residentId);

    @Query("SELECT gi FROM GroupInvitation gi WHERE gi.groupId = :groupId AND gi.status = 'PENDING'")
    List<GroupInvitation> findPendingInvitationsByGroupId(@Param("groupId") UUID groupId);

    @Query("SELECT gi FROM GroupInvitation gi WHERE gi.groupId = :groupId AND (gi.status = 'PENDING' OR gi.status = 'ACCEPTED')")
    List<GroupInvitation> findInvitationsByGroupId(@Param("groupId") UUID groupId);

    Optional<GroupInvitation> findByIdAndStatus(UUID id, String status);

    @Query("SELECT gi FROM GroupInvitation gi WHERE gi.groupId = :groupId AND gi.inviteePhone = :phone AND gi.status = 'PENDING'")
    Optional<GroupInvitation> findPendingByGroupIdAndPhone(@Param("groupId") UUID groupId, @Param("phone") String phone);
    
    @Query("SELECT gi FROM GroupInvitation gi WHERE gi.groupId = :groupId AND gi.inviteePhone = :phone ORDER BY gi.createdAt DESC")
    List<GroupInvitation> findByGroupIdAndPhone(@Param("groupId") UUID groupId, @Param("phone") String phone);
    
    @Query("SELECT gi FROM GroupInvitation gi WHERE gi.groupId = :groupId AND gi.inviterId = :inviterId AND gi.inviteeResidentId = :inviteeResidentId AND gi.status = 'PENDING'")
    Optional<GroupInvitation> findPendingByGroupIdAndInviterInvitee(@Param("groupId") UUID groupId, @Param("inviterId") UUID inviterId, @Param("inviteeResidentId") UUID inviteeResidentId);
    
    /**
     * Find PENDING group invitations from inviter to invitee (across all groups)
     * Used when blocker blocks blocked user - need to delete pending invitations
     */
    @Query("SELECT gi FROM GroupInvitation gi WHERE gi.inviterId = :inviterId AND gi.inviteeResidentId = :inviteeResidentId AND gi.status = 'PENDING'")
    List<GroupInvitation> findPendingInvitationsFromInviterToInvitee(
            @Param("inviterId") UUID inviterId,
            @Param("inviteeResidentId") UUID inviteeResidentId);
}

