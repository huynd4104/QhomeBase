package com.QhomeBase.chatservice.service;

import com.QhomeBase.chatservice.dto.*;
import com.QhomeBase.chatservice.model.Group;
import com.QhomeBase.chatservice.model.GroupMember;
import com.QhomeBase.chatservice.repository.GroupMemberRepository;
import com.QhomeBase.chatservice.repository.GroupRepository;
import com.QhomeBase.chatservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final com.QhomeBase.chatservice.repository.GroupInvitationRepository invitationRepository;
    private final ResidentInfoService residentInfoService;
    private final MessageService messageService;
    private final ChatNotificationService notificationService;

    @Value("${chat.group.max-members:30}")
    private Integer maxMembers;

    @Transactional
    public GroupResponse createGroup(CreateGroupRequest request, UUID userId) {
        // Get residentId from userId
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }

        // Create group
        Group group = Group.builder()
                .name(request.getName())
                .description(request.getDescription())
                .createdBy(residentId)
                .buildingId(request.getBuildingId())
                .maxMembers(maxMembers)
                .isActive(true)
                .build();

        group = groupRepository.save(group);

        // Add creator as ADMIN
        GroupMember creatorMember = GroupMember.builder()
                .group(group)
                .groupId(group.getId())
                .residentId(residentId)
                .role("ADMIN")
                .isMuted(false)
                .build();
        groupMemberRepository.save(creatorMember);

        // Add other members if provided
        if (request.getMemberIds() != null && !request.getMemberIds().isEmpty()) {
            long currentCount = groupMemberRepository.countByGroupId(group.getId());
            int remainingSlots = maxMembers - (int) currentCount;
            
            List<UUID> membersToAdd = request.getMemberIds().stream()
                    .limit(remainingSlots)
                    .collect(Collectors.toList());

            for (UUID memberId : membersToAdd) {
                if (!memberId.equals(residentId) && 
                    !groupMemberRepository.existsByGroupIdAndResidentId(group.getId(), memberId)) {
                    
                    // Check if this user (memberId) has already sent a PENDING invitation to the creator (residentId) for this group
                    // This handles the case: A invites B to group, B doesn't know, B creates new group and adds A
                    Optional<com.QhomeBase.chatservice.model.GroupInvitation> reverseInvitation = 
                            invitationRepository.findPendingByGroupIdAndInviterInvitee(group.getId(), memberId, residentId);
                    
                    if (reverseInvitation.isPresent()) {
                        // Get member name for error message
                        String memberName = residentInfoService.getResidentName(memberId, accessToken);
                        String errorMessage = memberName != null 
                            ? memberName + " đã gửi lời mời cho bạn rồi. Vui lòng vào mục lời mời để xác nhận."
                            : "Người dùng này đã gửi lời mời cho bạn rồi. Vui lòng vào mục lời mời để xác nhận.";
                        throw new RuntimeException(errorMessage);
                    }
                    
                    GroupMember member = GroupMember.builder()
                            .group(group)
                            .groupId(group.getId())
                            .residentId(memberId)
                            .role("MEMBER")
                            .isMuted(false)
                            .build();
                    groupMemberRepository.save(member);
                }
            }
        }

        return toGroupResponse(group, residentId);
    }

    @Transactional(readOnly = true)
    public GroupResponse getGroupById(UUID groupId, UUID userId) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }

        Group group = groupRepository.findActiveGroupById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found: " + groupId));

        // Check if user is a member (any role: ADMIN, MODERATOR, or MEMBER)
        // All members can view the group details including the full member list
        GroupMember member = groupMemberRepository.findByGroupIdAndResidentId(groupId, residentId)
                .orElseThrow(() -> new RuntimeException("You are not a member of this group"));

        return toGroupResponse(group, residentId);
    }

    @Transactional(readOnly = true)
    public GroupPagedResponse getGroupsByResident(UUID userId, int page, int size) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Group> groups = groupRepository.findGroupsByResidentId(residentId, pageable);

        Page<GroupResponse> responsePage = groups.map(group -> toGroupResponse(group, residentId));

        return GroupPagedResponse.builder()
                .content(responsePage.getContent())
                .currentPage(responsePage.getNumber())
                .pageSize(responsePage.getSize())
                .totalElements(responsePage.getTotalElements())
                .totalPages(responsePage.getTotalPages())
                .hasNext(responsePage.hasNext())
                .hasPrevious(responsePage.hasPrevious())
                .isFirst(responsePage.isFirst())
                .isLast(responsePage.isLast())
                .build();
    }

    @Transactional
    public GroupResponse updateGroup(UUID groupId, UpdateGroupRequest request, UUID userId) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }

        Group group = groupRepository.findActiveGroupById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found: " + groupId));

        // Check if user is a member of the group
        GroupMember member = groupMemberRepository.findByGroupIdAndResidentId(groupId, residentId)
                .orElseThrow(() -> new RuntimeException("You are not a member of this group"));

        // All members can update group name
        // Only admins can update description and avatar
        if (request.getName() != null) {
            group.setName(request.getName());
        }
        if (request.getDescription() != null) {
            if (!"ADMIN".equals(member.getRole())) {
                throw new RuntimeException("Only admins can update group description");
            }
            group.setDescription(request.getDescription());
        }
        if (request.getAvatarUrl() != null) {
            if (!"ADMIN".equals(member.getRole())) {
                throw new RuntimeException("Only admins can update group avatar");
            }
            group.setAvatarUrl(request.getAvatarUrl());
        }

        group = groupRepository.save(group);
        GroupResponse response = toGroupResponse(group, residentId);
        
        // Notify via WebSocket
        notificationService.notifyGroupUpdated(group.getId(), response);
        
        return response;
    }

    @Transactional
    public void addMembers(UUID groupId, AddMembersRequest request, UUID userId) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }

        Group group = groupRepository.findActiveGroupById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found: " + groupId));

        // Check if user is ADMIN or MODERATOR
        GroupMember member = groupMemberRepository.findByGroupIdAndResidentId(groupId, residentId)
                .orElseThrow(() -> new RuntimeException("You are not a member of this group"));

        if (!"ADMIN".equals(member.getRole()) && !"MODERATOR".equals(member.getRole())) {
            throw new RuntimeException("Only admins and moderators can add members");
        }

        long currentCount = groupMemberRepository.countByGroupId(groupId);
        int remainingSlots = group.getMaxMembers() - (int) currentCount;

        if (remainingSlots <= 0) {
            throw new RuntimeException("Group is full. Maximum " + group.getMaxMembers() + " members allowed.");
        }

        List<UUID> membersToAdd = request.getMemberIds().stream()
                .limit(remainingSlots)
                .filter(memberId -> !groupMemberRepository.existsByGroupIdAndResidentId(groupId, memberId))
                .collect(Collectors.toList());

        for (UUID memberId : membersToAdd) {
            // Check if this user (memberId) has already sent a PENDING invitation to the current user (residentId) for this group
            // This handles the case: A invites B to group, B doesn't know, B creates new group and adds A
            Optional<com.QhomeBase.chatservice.model.GroupInvitation> reverseInvitation = 
                    invitationRepository.findPendingByGroupIdAndInviterInvitee(groupId, memberId, residentId);
            
            if (reverseInvitation.isPresent()) {
                // Get member name for error message
                String memberName = residentInfoService.getResidentName(memberId, accessToken);
                String errorMessage = memberName != null 
                    ? memberName + " đã gửi lời mời cho bạn rồi. Vui lòng vào mục lời mời để xác nhận."
                    : "Người dùng này đã gửi lời mời cho bạn rồi. Vui lòng vào mục lời mời để xác nhận.";
                throw new RuntimeException(errorMessage);
            }
            
            GroupMember newMember = GroupMember.builder()
                    .group(group)
                    .groupId(group.getId())
                    .residentId(memberId)
                    .role("MEMBER")
                    .isMuted(false)
                    .build();
            groupMemberRepository.save(newMember);
            
            // Notify via WebSocket
            GroupMemberResponse memberResponse = toGroupMemberResponse(newMember);
            notificationService.notifyMemberAdded(groupId, memberResponse);
        }
    }

    @Transactional
    public void removeMember(UUID groupId, UUID memberId, UUID userId) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }

        Group group = groupRepository.findActiveGroupById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found: " + groupId));

        // Check if user is ADMIN or removing themselves
        GroupMember requester = groupMemberRepository.findByGroupIdAndResidentId(groupId, residentId)
                .orElseThrow(() -> new RuntimeException("You are not a member of this group"));

        if (!residentId.equals(memberId) && !"ADMIN".equals(requester.getRole())) {
            throw new RuntimeException("Only admins can remove other members");
        }

        GroupMember memberToRemove = groupMemberRepository.findByGroupIdAndResidentId(groupId, memberId)
                .orElseThrow(() -> new RuntimeException("Member not found in group"));

        if ("ADMIN".equals(memberToRemove.getRole())) {
            // Check if there's at least one other admin
            List<GroupMember> admins = groupMemberRepository.findAdminsByGroupId(groupId);
            if (admins.size() <= 1) {
                // Check if there are other members (non-admin)
                List<GroupMember> allMembers = groupMemberRepository.findByGroupId(groupId);
                long otherMembersCount = allMembers.stream()
                        .filter(m -> !m.getResidentId().equals(memberId))
                        .count();
                
                if (otherMembersCount > 0) {
                    // There are other members but no other admin - cannot remove
                    throw new RuntimeException("Cannot remove the last admin. Please promote another member to admin first, or transfer admin rights before leaving.");
                }
                // No other members - allow removal (group will be empty)
            }
        }

        UUID removedMemberId = memberToRemove.getResidentId();
        
        // Get member name before deleting
        Map<String, Object> memberInfo = residentInfoService.getResidentInfo(removedMemberId);
        String memberName = memberInfo != null ? (String) memberInfo.get("fullName") : "Một thành viên";
        
        groupMemberRepository.delete(memberToRemove);
        
        // Create system message
        String systemMessageContent = memberName + " đã rời khỏi nhóm";
        try {
            messageService.createSystemMessage(groupId, systemMessageContent);
        } catch (Exception e) {
            log.warn("Failed to create system message for member leave: {}", e.getMessage());
        }
        
        // Notify via WebSocket
        notificationService.notifyMemberRemoved(groupId, removedMemberId);
    }

    @Transactional
    public void leaveGroup(UUID groupId, UUID userId) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }
        
        // Check if user is the last admin before attempting to leave
        GroupMember currentMember = groupMemberRepository.findByGroupIdAndResidentId(groupId, residentId)
                .orElseThrow(() -> new RuntimeException("You are not a member of this group"));
        
        if ("ADMIN".equals(currentMember.getRole())) {
            List<GroupMember> admins = groupMemberRepository.findAdminsByGroupId(groupId);
            if (admins.size() <= 1) {
                // User is the last admin - need to promote someone else or handle differently
                List<GroupMember> allMembers = groupMemberRepository.findByGroupId(groupId);
                
                // Filter out the current user
                List<GroupMember> otherMembers = allMembers.stream()
                        .filter(m -> !m.getResidentId().equals(residentId))
                        .collect(Collectors.toList());
                
                if (!otherMembers.isEmpty()) {
                    // Promote the first MODERATOR, or if none, the oldest MEMBER
                    GroupMember memberToPromote = otherMembers.stream()
                            .filter(m -> "MODERATOR".equals(m.getRole()))
                            .findFirst()
                            .orElse(otherMembers.stream()
                                    .min((m1, m2) -> m1.getJoinedAt().compareTo(m2.getJoinedAt()))
                                    .orElse(null));
                    
                    if (memberToPromote != null) {
                        memberToPromote.setRole("ADMIN");
                        groupMemberRepository.save(memberToPromote);
                        log.info("Promoted member {} to ADMIN in group {} as the previous admin is leaving", 
                                memberToPromote.getResidentId(), groupId);
                    }
                } else {
                    // No other members - allow leaving (group will be empty)
                    log.info("Last admin leaving group {} with no other members", groupId);
                }
            }
        }
        
        // Now proceed with removal
        removeMember(groupId, residentId, userId);
    }

    @Transactional
    public void deleteGroup(UUID groupId, UUID userId) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }

        Group group = groupRepository.findActiveGroupById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found: " + groupId));

        // Check if user is a member
        GroupMember member = groupMemberRepository.findByGroupIdAndResidentId(groupId, residentId)
                .orElseThrow(() -> new RuntimeException("You are not a member of this group"));

        OffsetDateTime deleteTime = OffsetDateTime.now();

        // Hide the group for this user (set hidden_at)
        member.setHiddenAt(deleteTime);
        groupMemberRepository.save(member);

        // Delete all messages sent by this user from the deletion time onwards
        // Actually, we should delete all messages from this user regardless of time
        // But the requirement says "from the deletion time onwards", so we'll delete messages created after deleteTime
        // However, to be safe, let's delete all messages from this user in this group
        messageService.deleteUserMessagesFromGroup(groupId, residentId, deleteTime);

        // Delete all files/images uploaded by this user in this group
        // Note: We'll mark them as deleted in the database, actual file deletion can be handled separately
        messageService.deleteUserFilesFromGroup(groupId, residentId, deleteTime);

        log.info("User {} hid group {} and deleted their messages/files from {}", residentId, groupId, deleteTime);
        
        // Note: We don't notify via WebSocket because the group is only hidden for this user
        // Other users can still see the group and messages
    }

    private GroupResponse toGroupResponse(Group group, UUID currentResidentId) {
        // Get all members of the group (ADMIN, MODERATOR, MEMBER) - no role filtering
        // All group members can see all other members
        List<GroupMember> members = groupMemberRepository.findByGroupId(group.getId());
        
        String userRole = null;
        Long unreadCount = 0L;
        GroupMember currentMember = groupMemberRepository.findByGroupIdAndResidentId(group.getId(), currentResidentId).orElse(null);
        if (currentMember != null) {
            userRole = currentMember.getRole();
            // Calculate unread count (messages after lastReadAt, excluding own messages)
            unreadCount = messageService.countUnreadMessages(
                group.getId(), 
                currentMember.getLastReadAt(), 
                currentResidentId // Exclude messages sent by current user
            );
        }

        List<GroupMemberResponse> memberResponses = members.stream()
                .map(this::toGroupMemberResponse)
                .collect(Collectors.toList());

        // Get creator info
        Map<String, Object> creatorInfo = residentInfoService.getResidentInfo(group.getCreatedBy());
        String createdByName = creatorInfo != null ? (String) creatorInfo.get("fullName") : null;

        // Get building name if buildingId exists
        String buildingName = null;
        if (group.getBuildingId() != null) {
            // TODO: Call base-service to get building name
        }

        return GroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .createdBy(group.getCreatedBy())
                .createdByName(createdByName)
                .buildingId(group.getBuildingId())
                .buildingName(buildingName)
                .avatarUrl(group.getAvatarUrl())
                .maxMembers(group.getMaxMembers())
                .currentMemberCount(members.size())
                .isActive(group.getIsActive())
                .createdAt(group.getCreatedAt())
                .updatedAt(group.getUpdatedAt())
                .members(memberResponses)
                .userRole(userRole)
                .unreadCount(unreadCount)
                .build();
    }

    private GroupMemberResponse toGroupMemberResponse(GroupMember member) {
        Map<String, Object> residentInfo = residentInfoService.getResidentInfo(member.getResidentId());
        String residentName = residentInfo != null ? (String) residentInfo.get("fullName") : null;
        String residentAvatar = null; // TODO: Get avatar from resident info

        return GroupMemberResponse.builder()
                .id(member.getId())
                .groupId(member.getGroupId())
                .residentId(member.getResidentId())
                .residentName(residentName)
                .residentAvatar(residentAvatar)
                .role(member.getRole())
                .joinedAt(member.getJoinedAt())
                .lastReadAt(member.getLastReadAt())
                .isMuted(member.getIsMuted())
                .build();
    }

    private String getCurrentAccessToken() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
                UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
                return principal.token();
            }
        } catch (Exception e) {
            log.debug("Could not get token from SecurityContext: {}", e.getMessage());
        }
        return null;
    }
}

