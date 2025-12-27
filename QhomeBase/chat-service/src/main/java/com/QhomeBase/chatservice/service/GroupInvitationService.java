package com.QhomeBase.chatservice.service;

import com.QhomeBase.chatservice.dto.GroupInvitationResponse;
import com.QhomeBase.chatservice.dto.InviteMembersByPhoneRequest;
import com.QhomeBase.chatservice.dto.InviteMembersResponse;
import com.QhomeBase.chatservice.model.Group;
import com.QhomeBase.chatservice.model.GroupInvitation;
import com.QhomeBase.chatservice.model.GroupMember;
import com.QhomeBase.chatservice.repository.GroupInvitationRepository;
import com.QhomeBase.chatservice.repository.GroupMemberRepository;
import com.QhomeBase.chatservice.repository.GroupRepository;
import com.QhomeBase.chatservice.repository.BlockRepository;
import com.QhomeBase.chatservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupInvitationService {

    private final GroupInvitationRepository invitationRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ResidentInfoService residentInfoService;
    private final ChatNotificationService notificationService;
    private final FcmPushService fcmPushService;
    private final BlockRepository blockRepository;

    @Value("${base.service.url:http://localhost:8081}")
    private String baseServiceUrl;

    private final WebClient webClient = WebClient.builder().build();

    /**
     * Find resident by phone number from base-service
     */
    private UUID findResidentIdByPhone(String phone, String accessToken) {
        // Normalize phone: remove all non-digit characters
        String normalizedPhone = phone.replaceAll("[^0-9]", "");
        
        // Try multiple formats
        List<String> phoneFormats = new java.util.ArrayList<>();
        phoneFormats.add(normalizedPhone); // Original format
        if (!normalizedPhone.startsWith("0") && normalizedPhone.length() > 0) {
            phoneFormats.add("0" + normalizedPhone); // With leading zero
        }
        if (normalizedPhone.startsWith("0") && normalizedPhone.length() > 1) {
            phoneFormats.add(normalizedPhone.substring(1)); // Without leading zero
        }
        
        for (String phoneFormat : phoneFormats) {
            try {
                // Use UriComponentsBuilder to properly construct the URL
                String url = UriComponentsBuilder
                        .fromUriString(baseServiceUrl)
                        .path("/api/residents/by-phone/{phone}")
                        .buildAndExpand(phoneFormat)
                        .toUriString();
                
                log.debug("Trying to find resident by phone format: {}", phoneFormat);
                log.debug("Full URL: {}", url);
                
                @SuppressWarnings("unchecked")
                Map<String, Object> response = (Map<String, Object>) webClient
                        .get()
                        .uri(url)
                        .header("Authorization", "Bearer " + accessToken)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();
                
                if (response != null) {
                    Object idObj = response.get("id");
                    if (idObj != null) {
                        UUID residentId = UUID.fromString(idObj.toString());
                        log.info("Found resident by phone format '{}': residentId={}", phoneFormat, residentId);
                        return residentId;
                    }
                }
            } catch (Exception e) {
                log.debug("Resident not found for phone format: {} - {}", phoneFormat, e.getMessage());
                // Continue to try next format
            }
        }
        
        log.warn("Resident not found for phone '{}' (tried formats: {})", phone, phoneFormats);
        return null;
    }

    /**
     * Invite members to group by phone numbers
     */
    @Transactional
    public InviteMembersResponse inviteMembersByPhone(UUID groupId, InviteMembersByPhoneRequest request, UUID userId) {
        String accessToken = getCurrentAccessToken();
        UUID inviterResidentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (inviterResidentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }

        Group group = groupRepository.findActiveGroupById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found: " + groupId));

        // Check if user is ADMIN or MODERATOR
        GroupMember member = groupMemberRepository.findByGroupIdAndResidentId(groupId, inviterResidentId)
                .orElseThrow(() -> new RuntimeException("You are not a member of this group"));

        if (!"ADMIN".equals(member.getRole()) && !"MODERATOR".equals(member.getRole())) {
            throw new RuntimeException("Only admins and moderators can invite members");
        }

        // Check group capacity
        long currentCount = groupMemberRepository.countByGroupId(groupId);
        int remainingSlots = group.getMaxMembers() - (int) currentCount;

        if (remainingSlots <= 0) {
            throw new RuntimeException("Group is full. Maximum " + group.getMaxMembers() + " members allowed.");
        }

        List<GroupInvitationResponse> successfulInvitations = new ArrayList<>();
        List<String> invalidPhones = new ArrayList<>();
        List<String> skippedPhones = new ArrayList<>();
        
        Map<String, Object> inviterInfo = residentInfoService.getResidentInfo(inviterResidentId);
        String inviterName = inviterInfo != null ? (String) inviterInfo.get("fullName") : null;

        for (String phone : request.getPhoneNumbers()) {
            if (successfulInvitations.size() >= remainingSlots) {
                skippedPhones.add(phone + " (Nhóm đã đầy)");
                continue; // Group is full
            }

            // Normalize phone number (remove spaces, dashes, etc.)
            String normalizedPhone = phone.replaceAll("[^0-9]", "");
            
            // Validate phone number format (should be 9-10 digits after normalization)
            if (normalizedPhone.length() < 9 || normalizedPhone.length() > 11) {
                invalidPhones.add(phone + " (Định dạng không hợp lệ)");
                continue;
            }
            
            // Ensure consistent format: remove leading zero if present for storage
            // Vietnamese phones: 0123456789 -> 123456789 for consistency
            String phoneForStorage = normalizedPhone;
            if (normalizedPhone.startsWith("0") && normalizedPhone.length() > 1) {
                phoneForStorage = normalizedPhone.substring(1);
            }
            
            log.debug("Normalized phone from '{}' to '{}' (storage: '{}')", phone, normalizedPhone, phoneForStorage);

            // Validate: Check if phone number exists in database
            UUID residentId = findResidentIdByPhone(phoneForStorage, accessToken);
            if (residentId == null && !phoneForStorage.startsWith("0")) {
                // Try with leading zero
                residentId = findResidentIdByPhone("0" + phoneForStorage, accessToken);
            }
            
            if (residentId == null) {
                // Phone number doesn't exist in database
                invalidPhones.add(phone + " (Số điện thoại không tồn tại trong hệ thống)");
                log.warn("Phone number '{}' (normalized: '{}') not found in database", phone, phoneForStorage);
                continue;
            }

            // Check if already a member
            if (groupMemberRepository.existsByGroupIdAndResidentId(groupId, residentId)) {
                skippedPhones.add(phone + " (Đã là thành viên)");
                log.info("Resident {} is already a member of group {}", residentId, groupId);
                continue;
            }
            
            // Check if this user (residentId) has already sent a PENDING invitation to the inviter for this group
            // This handles the case: A invites B to group, B doesn't know, B invites A to same group
            Optional<GroupInvitation> reverseInvitation = invitationRepository.findPendingByGroupIdAndInviterInvitee(
                    groupId, residentId, inviterResidentId);
            if (reverseInvitation.isPresent()) {
                // Get invitee name (the person who sent the reverse invitation)
                Map<String, Object> inviteeInfo = residentInfoService.getResidentInfo(residentId);
                String inviteeName = inviteeInfo != null ? (String) inviteeInfo.get("fullName") : null;
                String message = inviteeName != null 
                    ? inviteeName + " đã gửi lời mời cho bạn rồi. Vui lòng vào mục lời mời để xác nhận."
                    : "Người dùng này đã gửi lời mời cho bạn rồi. Vui lòng vào mục lời mời để xác nhận.";
                skippedPhones.add(phone + " (" + message + ")");
                log.info("Resident {} has already sent PENDING invitation to {} for group {}. Skipping.", 
                        residentId, inviterResidentId, groupId);
                continue;
            }

            // Check if there's already a PENDING invitation from inviter to this phone
            // If A already sent PENDING invitation to B, skip and inform A (don't throw exception to allow processing other phones)
            Optional<GroupInvitation> existingPending = invitationRepository.findPendingByGroupIdAndPhone(groupId, phoneForStorage);
            if (existingPending.isEmpty() && !phoneForStorage.startsWith("0")) {
                // Also try with leading zero
                existingPending = invitationRepository.findPendingByGroupIdAndPhone(groupId, "0" + phoneForStorage);
            }
            if (existingPending.isPresent()) {
                // Check if this is the same inviter (A sending invitation to B again)
                if (existingPending.get().getInviterId().equals(inviterResidentId)) {
                    log.info("⚠️ User {} already sent PENDING invitation to phone {} for group {}. Skipping duplicate invitation.", 
                            inviterResidentId, phoneForStorage, groupId);
                    log.info("   Existing invitation details - ID: {}, Status: {}, Inviter: {}, InviteePhone: {}", 
                            existingPending.get().getId(), existingPending.get().getStatus(), 
                            existingPending.get().getInviterId(), existingPending.get().getInviteePhone());
                    // Add to skippedPhones with clear message instead of throwing exception
                    // This allows processing other phone numbers in the same request
                    skippedPhones.add(phone + " (Bạn đã gửi lời mời rồi. Vui lòng đợi phản hồi từ người dùng.)");
                    continue;
                } else {
                    // Different inviter - this is a reverse invitation case, skip with message
                    skippedPhones.add(phone + " (Đã có lời mời đang chờ)");
                    log.info("Pending invitation already exists for phone {} in group {} (from different inviter)", phoneForStorage, groupId);
                    continue;
                }
            }
            
            // Check if inviter has blocked the invitee or vice versa
            if (blockRepository.findByBlockerIdAndBlockedId(inviterResidentId, residentId).isPresent() ||
                blockRepository.findByBlockerIdAndBlockedId(residentId, inviterResidentId).isPresent()) {
                skippedPhones.add(phone + " (Người dùng đã bị chặn)");
                log.info("Cannot invite resident {} to group {}: blocked relationship exists", residentId, groupId);
                continue;
            }

            // Create new invitation (invitations don't expire, so we always create new ones)
            // DECLINED invitations are deleted (like direct invitations), so we don't need to check for them
            GroupInvitation invitation = GroupInvitation.builder()
                    .group(group)
                    .groupId(groupId)
                    .inviterId(inviterResidentId)
                    .inviteePhone(phoneForStorage)
                    .inviteeResidentId(residentId)
                    .status("PENDING")
                    .build();
            invitation = invitationRepository.save(invitation);

            log.info("✅ [GroupInvitationService] Created invitation - ID: {}, GroupId: {}, InviteeResidentId: {}, InviteePhone: {}, Status: {}", 
                invitation.getId(), groupId, residentId, phoneForStorage, invitation.getStatus());

            // Send notification
            String title = "Lời mời tham gia nhóm";
            String body = inviterName != null 
                ? inviterName + " mời bạn tham gia nhóm \"" + group.getName() + "\""
                : "Bạn được mời tham gia nhóm \"" + group.getName() + "\"";

            Map<String, String> data = new java.util.HashMap<>();
            data.put("type", "GROUP_INVITATION");
            data.put("groupId", groupId.toString());
            data.put("groupName", group.getName());
            data.put("invitationId", invitation.getId().toString());
            data.put("inviterId", inviterResidentId.toString());
            data.put("inviterName", inviterName != null ? inviterName : "");

            // Send FCM push notification
            try {
                fcmPushService.sendPushToResident(residentId, title, body, data);
                log.info("📱 [GroupInvitationService] FCM push notification sent to residentId: {}", residentId);
            } catch (Exception e) {
                log.error("❌ [GroupInvitationService] Failed to send FCM push notification to residentId: {}", residentId, e);
            }

            // Send WebSocket notification
            // TODO: Add WebSocket notification for invitation

            GroupInvitationResponse response = GroupInvitationResponse.builder()
                    .id(invitation.getId())
                    .groupId(groupId)
                    .groupName(group.getName())
                    .inviterId(inviterResidentId)
                    .inviterName(inviterName)
                    .inviteePhone(phoneForStorage)
                    .inviteeResidentId(residentId)
                    .status(invitation.getStatus())
                    .createdAt(invitation.getCreatedAt())
                    .expiresAt(invitation.getExpiresAt())
                    .build();

            successfulInvitations.add(response);
            log.info("✅ [GroupInvitationService] Successfully created invitation for phone {} (normalized: {}, residentId: {}, invitationId: {})", 
                phone, phoneForStorage, residentId, invitation.getId());
        }

        return InviteMembersResponse.builder()
                .successfulInvitations(successfulInvitations)
                .invalidPhones(invalidPhones)
                .skippedPhones(skippedPhones)
                .build();
    }

    /**
     * Get pending invitations for current user
     */
    @Transactional(readOnly = true)
    public List<GroupInvitationResponse> getMyPendingInvitations(UUID userId) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }

        List<GroupInvitation> invitations = new ArrayList<>();

        // Get resident phone first (needed for phone-based search)
        Map<String, Object> residentInfo = residentInfoService.getResidentInfo(residentId);
        String phone = residentInfo != null ? (String) residentInfo.get("phone") : null;
        
        log.info("Getting pending invitations for residentId: {}, phone: {}", residentId, phone);

        // First, try to find by residentId (if invitation was created after resident was found)
        List<GroupInvitation> byResidentId = invitationRepository.findPendingInvitationsByResidentId(residentId);
        log.info("Found {} invitations by residentId: {}", byResidentId.size(), residentId);
        
        // Debug: Log all pending invitations for this residentId
        if (!byResidentId.isEmpty()) {
            for (GroupInvitation inv : byResidentId) {
                log.info("Found invitation by residentId - ID: {}, Phone: {}, GroupId: {}, Status: {}, InviteeResidentId: {}", 
                    inv.getId(), inv.getInviteePhone(), inv.getGroupId(), inv.getStatus(), inv.getInviteeResidentId());
                if (!invitations.contains(inv)) {
                    invitations.add(inv);
                }
            }
        }

        // Also try to find by phone number (for invitations created before resident was found, 
        // or when inviteeResidentId doesn't match current residentId)
        if (phone != null && !phone.isEmpty()) {
            // Normalize phone number (remove all non-digit characters)
            String normalizedPhone = phone.replaceAll("[^0-9]", "");
            
            log.info("Looking for invitations with phone: {} (normalized: {}) for residentId: {}", phone, normalizedPhone, residentId);
            
            // Try all possible formats that could have been stored
            // Format 1: Exact match (as stored in user DB)
            List<GroupInvitation> byPhone = invitationRepository.findPendingInvitationsByPhone(normalizedPhone);
            log.info("Found {} invitations with exact phone match: {}", byPhone.size(), normalizedPhone);
            for (GroupInvitation inv : byPhone) {
                log.info("  - Invitation ID: {}, Phone: {}, InviteeResidentId: {}", inv.getId(), inv.getInviteePhone(), inv.getInviteeResidentId());
            }
            
            // Format 2: With leading zero (if phone doesn't start with 0)
            String withLeadingZero = normalizedPhone.startsWith("0") ? normalizedPhone : "0" + normalizedPhone;
            List<GroupInvitation> byPhoneWithZero = invitationRepository.findPendingInvitationsByPhone(withLeadingZero);
            log.info("Found {} invitations with leading zero: {}", byPhoneWithZero.size(), withLeadingZero);
            for (GroupInvitation inv : byPhoneWithZero) {
                log.info("  - Invitation ID: {}, Phone: {}, InviteeResidentId: {}", inv.getId(), inv.getInviteePhone(), inv.getInviteeResidentId());
            }
            
            // Format 3: Without leading zero (storage format - this is how invitations are stored)
            String withoutLeadingZero = normalizedPhone.startsWith("0") && normalizedPhone.length() > 1 
                ? normalizedPhone.substring(1) 
                : normalizedPhone;
            List<GroupInvitation> byPhoneWithoutZero = invitationRepository.findPendingInvitationsByPhone(withoutLeadingZero);
            log.info("Found {} invitations without leading zero (storage format): {}", byPhoneWithoutZero.size(), withoutLeadingZero);
            for (GroupInvitation inv : byPhoneWithoutZero) {
                log.info("  - Invitation ID: {}, Phone: {}, InviteeResidentId: {}", inv.getId(), inv.getInviteePhone(), inv.getInviteeResidentId());
            }
            
            // Merge results, avoiding duplicates
            for (GroupInvitation inv : byPhone) {
                if (!invitations.contains(inv)) {
                    invitations.add(inv);
                    log.info("Added invitation {} by exact phone match (Phone: {}, InviteeResidentId: {})", 
                        inv.getId(), inv.getInviteePhone(), inv.getInviteeResidentId());
                }
            }
            for (GroupInvitation inv : byPhoneWithZero) {
                if (!invitations.contains(inv)) {
                    invitations.add(inv);
                    log.info("Added invitation {} by phone with leading zero (Phone: {}, InviteeResidentId: {})", 
                        inv.getId(), inv.getInviteePhone(), inv.getInviteeResidentId());
                }
            }
            for (GroupInvitation inv : byPhoneWithoutZero) {
                if (!invitations.contains(inv)) {
                    invitations.add(inv);
                    log.info("Added invitation {} by phone without leading zero (Phone: {}, InviteeResidentId: {})", 
                        inv.getId(), inv.getInviteePhone(), inv.getInviteeResidentId());
                }
            }
            
            log.info("Found {} invitations by phone (total unique after merge: {})", 
                byPhone.size() + byPhoneWithZero.size() + byPhoneWithoutZero.size(), invitations.size());
        } else {
            log.warn("Phone number not found for residentId: {}", residentId);
        }

        log.info("Total pending invitations found for residentId {}: {}", residentId, invitations.size());
        
        // Final debug: Log all found invitations
        if (!invitations.isEmpty()) {
            log.info("Final invitation list:");
            for (GroupInvitation inv : invitations) {
                log.info("  - ID: {}, Phone: {}, InviteeResidentId: {}, GroupId: {}, Status: {}", 
                    inv.getId(), inv.getInviteePhone(), inv.getInviteeResidentId(), inv.getGroupId(), inv.getStatus());
            }
        } else {
            log.warn("No invitations found for residentId: {}, phone: {}", residentId, phone);
        }

        return invitations.stream()
                .map(this::toInvitationResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all invitations for a specific group (PENDING and ACCEPTED)
     * Includes invitations sent by current user (as inviter) and received by current user (as invitee)
     */
    public List<GroupInvitationResponse> getGroupInvitations(UUID groupId, UUID userId) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }

        // Get all invitations for this group with PENDING or ACCEPTED status
        List<GroupInvitation> allGroupInvitations = invitationRepository.findInvitationsByGroupId(groupId);
        
        log.info("Found {} invitations (PENDING/ACCEPTED) for groupId: {}", allGroupInvitations.size(), groupId);
        
        return allGroupInvitations.stream()
                .map(this::toInvitationResponse)
                .collect(Collectors.toList());
    }

    /**
     * Accept invitation
     */
    @Transactional
    public void acceptInvitation(UUID invitationId, UUID userId) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }

        GroupInvitation invitation = invitationRepository.findByIdAndStatus(invitationId, "PENDING")
                .orElseThrow(() -> new RuntimeException("Invitation not found or already processed"));

        // Verify this invitation is for the current user
        // First check by residentId (most reliable)
        if (invitation.getInviteeResidentId() != null) {
            if (!invitation.getInviteeResidentId().equals(residentId)) {
                throw new RuntimeException("This invitation is not for you");
            }
        } else {
            // Fallback: check by phone number if residentId is not set (for old invitations)
            Map<String, Object> residentInfo = residentInfoService.getResidentInfo(residentId);
            String phone = residentInfo != null ? (String) residentInfo.get("phone") : null;
            if (phone == null) {
                throw new RuntimeException("Phone number not found for resident");
            }

            String normalizedPhone = phone.replaceAll("[^0-9]", "");
            // Remove leading zero for comparison (storage format)
            String phoneForComparison = normalizedPhone.startsWith("0") && normalizedPhone.length() > 1
                ? normalizedPhone.substring(1)
                : normalizedPhone;
            
            if (!phoneForComparison.equals(invitation.getInviteePhone())) {
                throw new RuntimeException("This invitation is not for you");
            }
        }

        // Check if already a member
        if (groupMemberRepository.existsByGroupIdAndResidentId(invitation.getGroupId(), residentId)) {
            invitation.setStatus("ACCEPTED");
            invitation.setRespondedAt(OffsetDateTime.now());
            invitationRepository.save(invitation);
            return; // Already a member
        }

        // Check group capacity
        Group group = groupRepository.findActiveGroupById(invitation.getGroupId())
                .orElseThrow(() -> new RuntimeException("Group not found"));
        
        long currentCount = groupMemberRepository.countByGroupId(invitation.getGroupId());
        if (currentCount >= group.getMaxMembers()) {
            throw new RuntimeException("Group is full");
        }

        // Add as member
        GroupMember newMember = GroupMember.builder()
                .group(group)
                .groupId(invitation.getGroupId())
                .residentId(residentId)
                .role("MEMBER")
                .isMuted(false)
                .build();
        groupMemberRepository.save(newMember);

        // Update invitation
        invitation.setStatus("ACCEPTED");
        invitation.setInviteeResidentId(residentId);
        invitation.setRespondedAt(OffsetDateTime.now());
        invitationRepository.save(invitation);

        // Notify via WebSocket
        com.QhomeBase.chatservice.dto.GroupMemberResponse memberResponse = toGroupMemberResponse(newMember);
        notificationService.notifyMemberAdded(invitation.getGroupId(), memberResponse);
    }

    /**
     * Decline invitation
     */
    @Transactional
    public void declineInvitation(UUID invitationId, UUID userId) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }

        GroupInvitation invitation = invitationRepository.findByIdAndStatus(invitationId, "PENDING")
                .orElseThrow(() -> new RuntimeException("Invitation not found or already processed"));

        // Verify this invitation is for the current user
        // First check by residentId (most reliable)
        if (invitation.getInviteeResidentId() != null) {
            if (!invitation.getInviteeResidentId().equals(residentId)) {
                throw new RuntimeException("This invitation is not for you");
            }
        } else {
            // Fallback: check by phone number if residentId is not set (for old invitations)
            Map<String, Object> residentInfo = residentInfoService.getResidentInfo(residentId);
            String phone = residentInfo != null ? (String) residentInfo.get("phone") : null;
            if (phone == null) {
                throw new RuntimeException("Phone number not found for resident");
            }

            String normalizedPhone = phone.replaceAll("[^0-9]", "");
            // Remove leading zero for comparison (storage format)
            String phoneForComparison = normalizedPhone.startsWith("0") && normalizedPhone.length() > 1
                ? normalizedPhone.substring(1)
                : normalizedPhone;
            
            if (!phoneForComparison.equals(invitation.getInviteePhone())) {
                throw new RuntimeException("This invitation is not for you");
            }
        }

        // Delete invitation when declined - this resets state to "chưa gửi lời mời"
        // After decline, users are in "chưa gửi lời mời" state and can send new invitation
        // This matches the behavior of direct chat invitations
        UUID inviterId = invitation.getInviterId();
        UUID groupId = invitation.getGroupId();
        UUID deletedInvitationId = invitation.getId();
        
        // Delete the invitation
        invitationRepository.delete(invitation);
        log.info("Deleted group invitation {} after decline (Inviter: {}, GroupId: {}, Invitee: {}). Users are now in 'chưa gửi lời mời' state.", 
                deletedInvitationId, inviterId, groupId, residentId);
        
        // Notify inviter via WebSocket (if notification service supports group invitations)
        // Note: Group invitation declined notification may need to be implemented separately
        try {
            // TODO: Implement group invitation declined notification if needed
            log.debug("Group invitation declined notification not yet implemented");
        } catch (Exception e) {
            log.warn("Failed to send group invitation declined notification: {}", e.getMessage());
        }
    }

    private GroupInvitationResponse toInvitationResponse(GroupInvitation invitation) {
        Map<String, Object> inviterInfo = residentInfoService.getResidentInfo(invitation.getInviterId());
        String inviterName = inviterInfo != null ? (String) inviterInfo.get("fullName") : null;

        Group group = groupRepository.findById(invitation.getGroupId()).orElse(null);
        String groupName = group != null ? group.getName() : null;

        return GroupInvitationResponse.builder()
                .id(invitation.getId())
                .groupId(invitation.getGroupId())
                .groupName(groupName)
                .inviterId(invitation.getInviterId())
                .inviterName(inviterName)
                .inviteePhone(invitation.getInviteePhone())
                .inviteeResidentId(invitation.getInviteeResidentId())
                .status(invitation.getStatus())
                .createdAt(invitation.getCreatedAt())
                .expiresAt(invitation.getExpiresAt())
                .build();
    }

    private com.QhomeBase.chatservice.dto.GroupMemberResponse toGroupMemberResponse(GroupMember member) {
        Map<String, Object> residentInfo = residentInfoService.getResidentInfo(member.getResidentId());
        String residentName = residentInfo != null ? (String) residentInfo.get("fullName") : null;
        String residentAvatar = null; // TODO: Get avatar from resident info

        return com.QhomeBase.chatservice.dto.GroupMemberResponse.builder()
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

