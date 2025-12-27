package com.QhomeBase.chatservice.service;

import com.QhomeBase.chatservice.dto.CreateDirectInvitationRequest;
import com.QhomeBase.chatservice.dto.DirectInvitationResponse;
import com.QhomeBase.chatservice.model.Conversation;
import com.QhomeBase.chatservice.model.ConversationParticipant;
import com.QhomeBase.chatservice.model.DirectInvitation;
import com.QhomeBase.chatservice.model.DirectMessage;
import com.QhomeBase.chatservice.repository.BlockRepository;
import com.QhomeBase.chatservice.repository.ConversationParticipantRepository;
import com.QhomeBase.chatservice.repository.ConversationRepository;
import com.QhomeBase.chatservice.repository.DirectInvitationRepository;
import com.QhomeBase.chatservice.repository.DirectMessageRepository;
import com.QhomeBase.chatservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

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
public class DirectInvitationService {

    private final DirectInvitationRepository invitationRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final DirectMessageRepository messageRepository;
    private final BlockRepository blockRepository;
    private final ResidentInfoService residentInfoService;
    private final ChatNotificationService notificationService;
    private final FcmPushService fcmPushService;
    private final FriendshipService friendshipService;

    @Value("${base.service.url:http://localhost:8081}")
    private String baseServiceUrl;

    private final WebClient webClient = WebClient.builder().build();

    private String getCurrentAccessToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal.token();
        }
        return null;
    }

    /**
     * Find resident ID by phone number
     */
    private UUID findResidentIdByPhone(String phone, String accessToken) {
        try {
            String url = baseServiceUrl + "/api/residents/by-phone/" + phone;
            
            Map<String, Object> response = webClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            
            if (response == null) {
                return null;
            }
            
            Object idObj = response.get("id");
            if (idObj == null) {
                return null;
            }
            
            return UUID.fromString(idObj.toString());
        } catch (Exception e) {
            log.debug("Resident not found for phone: {}", phone);
            return null;
        }
    }

    /**
     * Create a direct chat invitation
     * Rules:
     * - Inviter can only send ONE initial message
     * - Conversation is created with status PENDING
     * - Invitation expires after 7 days
     */
    @Transactional
    public DirectInvitationResponse createInvitation(
            UUID inviterId,
            CreateDirectInvitationRequest request) {
        
        String accessToken = getCurrentAccessToken();
        UUID inviterResidentId = residentInfoService.getResidentIdFromUserId(inviterId, accessToken);
        if (inviterResidentId == null) {
            throw new RuntimeException("Resident not found for user: " + inviterId);
        }

        // Determine inviteeResidentId from either phoneNumber or inviteeId
        UUID inviteeResidentId = null;
        
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().trim().isEmpty()) {
            // Use phone number to find resident
            String phone = request.getPhoneNumber().trim().replaceAll("[^0-9]", "");
            
            // Normalize phone: remove leading zero if present for storage consistency
            String phoneForStorage = phone;
            if (phone.startsWith("0") && phone.length() > 1) {
                phoneForStorage = phone.substring(1);
            }
            
            // Try to find resident by phone
            inviteeResidentId = findResidentIdByPhone(phoneForStorage, accessToken);
            if (inviteeResidentId == null && !phoneForStorage.startsWith("0")) {
                // Try with leading zero
                inviteeResidentId = findResidentIdByPhone("0" + phoneForStorage, accessToken);
            }
            
            if (inviteeResidentId == null) {
                throw new RuntimeException("Số điện thoại không tồn tại trong hệ thống: " + request.getPhoneNumber());
            }
            
            log.info("Found resident {} by phone number {}", inviteeResidentId, request.getPhoneNumber());
        } else if (request.getInviteeId() != null) {
            // Use inviteeId (might be userId or residentId)
            UUID inviteeIdFromRequest = request.getInviteeId();
            inviteeResidentId = residentInfoService.getResidentIdFromUserId(inviteeIdFromRequest, accessToken);
            if (inviteeResidentId == null) {
                // If conversion fails, assume it's already a residentId
                inviteeResidentId = inviteeIdFromRequest;
            }
        } else {
            throw new RuntimeException("Either phoneNumber or inviteeId must be provided");
        }
        
        log.info("Checking block status: inviterResidentId={}, inviteeResidentId={}", 
                inviterResidentId, inviteeResidentId);

        // Check if users are blocked (bidirectional)
        boolean inviterBlockedInvitee = blockRepository
                .findByBlockerIdAndBlockedId(inviterResidentId, inviteeResidentId).isPresent();
        boolean inviteeBlockedInviter = blockRepository
                .findByBlockerIdAndBlockedId(inviteeResidentId, inviterResidentId).isPresent();
        
        log.info("Block check result: inviterBlockedInvitee={}, inviteeBlockedInviter={}", 
                inviterBlockedInvitee, inviteeBlockedInviter);
        
        if (inviterBlockedInvitee || inviteeBlockedInviter) {
            String message = inviterBlockedInvitee 
                ? "Cannot send invitation: You have blocked this user"
                : "Cannot send invitation: This user has blocked you";
            log.warn("Block check failed: {}", message);
            throw new RuntimeException(message);
        }

        // Ensure participant1_id < participant2_id for uniqueness
        // PostgreSQL compares UUIDs lexicographically (as strings), so we use toString().compareTo()
        int comparison = inviterResidentId.toString().compareTo(inviteeResidentId.toString());
        UUID participant1Id = comparison < 0 ? inviterResidentId : inviteeResidentId;
        UUID participant2Id = comparison < 0 ? inviteeResidentId : inviterResidentId;
        
        // Log for debugging
        log.debug("Participant ordering: participant1Id={}, participant2Id={}, comparison={}", 
                participant1Id, participant2Id, comparison);
        
        // Final validation: ensure participant1Id < participant2Id
        if (participant1Id.toString().compareTo(participant2Id.toString()) >= 0) {
            log.error("CRITICAL: Participant ordering failed! participant1Id={}, participant2Id={}", 
                    participant1Id, participant2Id);
            // Swap to fix
            UUID temp = participant1Id;
            participant1Id = participant2Id;
            participant2Id = temp;
            log.warn("Swapped participants: participant1Id={}, participant2Id={}", 
                    participant1Id, participant2Id);
        }

        // Check if conversation already exists
        Conversation conversation = conversationRepository
                .findConversationBetweenParticipants(participant1Id, participant2Id)
                .orElse(null);

        if (conversation != null) {
            // If conversation status is DELETED (both participants have hidden it), reset to PENDING
            // This allows users to re-establish contact after hiding
            if ("DELETED".equals(conversation.getStatus())) {
                log.info("Conversation status is DELETED, resetting to PENDING to allow new invitations: {}", conversation.getId());
                conversation.setStatus("PENDING");
                conversation = conversationRepository.save(conversation);
                
                // Also unhide the conversation for both participants
                List<ConversationParticipant> participants = participantRepository.findByConversationId(conversation.getId());
                for (ConversationParticipant participant : participants) {
                    if (Boolean.TRUE.equals(participant.getIsHidden())) {
                        participant.setIsHidden(false);
                        participant.setHiddenAt(null);
                        participant.setLastReadAt(null);
                        participantRepository.save(participant);
                        log.info("Unhidden conversation {} for participant {}", conversation.getId(), participant.getResidentId());
                    }
                }
                
                // Reset any ACCEPTED invitations to PENDING so users can see and accept them again
                // Check for invitations from inviter to invitee and from invitee to inviter
                Optional<DirectInvitation> inv1 = invitationRepository
                        .findByConversationAndParticipants(conversation.getId(), inviterResidentId, inviteeResidentId);
                Optional<DirectInvitation> inv2 = invitationRepository
                        .findByConversationAndParticipants(conversation.getId(), inviteeResidentId, inviterResidentId);
                
                if (inv1.isPresent() && "ACCEPTED".equals(inv1.get().getStatus())) {
                    DirectInvitation inv = inv1.get();
                    log.info("Resetting ACCEPTED invitation {} to PENDING after conversation DELETED reset", inv.getId());
                    inv.setStatus("PENDING");
                    inv.setRespondedAt(null);
                    invitationRepository.save(inv);
                }
                
                if (inv2.isPresent() && "ACCEPTED".equals(inv2.get().getStatus())) {
                    DirectInvitation inv = inv2.get();
                    log.info("Resetting ACCEPTED invitation {} to PENDING after conversation DELETED reset", inv.getId());
                    inv.setStatus("PENDING");
                    inv.setRespondedAt(null);
                    invitationRepository.save(inv);
                }
            }
            
            // If conversation exists and is ACTIVE, check if users are actually friends
            // After block/unblock, conversation may be ACTIVE but friendship is inactive
            // In that case, don't auto-accept - require explicit acceptance
            if ("ACTIVE".equals(conversation.getStatus())) {
                // Check if users are actually friends (friendship is active)
                boolean areFriends = friendshipService.areFriends(inviterResidentId, inviteeResidentId);
                
                if (areFriends) {
                    // Users are friends - check for reverse invitation
                    Optional<DirectInvitation> reverseInvitationCheck = invitationRepository
                            .findByConversationAndParticipants(conversation.getId(), inviteeResidentId, inviterResidentId);
                    
                    if (reverseInvitationCheck.isPresent() && "PENDING".equals(reverseInvitationCheck.get().getStatus())) {
                        // Both users want to chat and are friends - auto-accept reverse invitation
                        log.info("Conversation ACTIVE, users are friends, and reverse invitation PENDING. Auto-accepting reverse invitation.");
                        
                        DirectInvitation reverseInv = reverseInvitationCheck.get();
                        reverseInv.setStatus("ACCEPTED");
                        reverseInv.setRespondedAt(OffsetDateTime.now());
                        invitationRepository.save(reverseInv);
                        log.info("Auto-accepted reverse invitation ID: {}", reverseInv.getId());
                        
                        // Notify both users
                        notificationService.notifyDirectInvitationAccepted(
                                reverseInv.getInviterId(),
                                conversation.getId(),
                                toResponse(reverseInv, accessToken));
                        
                        // Return the reverse invitation response
                        return toResponse(reverseInv, accessToken);
                    }
                }
                
                // Check if there's an existing invitation from inviter to invitee
                // This handles both cases: users are friends or not friends
                DirectInvitation existingInv = invitationRepository
                        .findByConversationAndParticipants(conversation.getId(), inviterResidentId, inviteeResidentId)
                        .orElse(null);
                
                if (existingInv != null) {
                    // Check if existing invitation is ACCEPTED - if so, reset to PENDING so user can see it
                    if ("ACCEPTED".equals(existingInv.getStatus())) {
                        log.info("Conversation ACTIVE but existing invitation is ACCEPTED. Resetting to PENDING so user can see it. Invitation ID: {}", existingInv.getId());
                        existingInv.setStatus("PENDING");
                        existingInv.setRespondedAt(null);
                        existingInv.setInitialMessage(request.getInitialMessage());
                        existingInv = invitationRepository.save(existingInv);
                        log.info("Reset invitation from ACCEPTED to PENDING. ID: {}", existingInv.getId());
                    }
                    // Return existing invitation response (now PENDING)
                    log.info("Conversation already ACTIVE, returning existing invitation ID: {}", existingInv.getId());
                    return toResponse(existingInv, accessToken);
                } else {
                    // Conversation ACTIVE but no invitation found - create new PENDING invitation
                    // This allows user to see the invitation even if conversation is ACTIVE
                    // After block/unblock, users are NOT friends, so invitation requires explicit acceptance
                    log.info("Conversation ACTIVE but no invitation found. Creating new PENDING invitation.");
                    DirectInvitation newInvitation = DirectInvitation.builder()
                            .conversation(conversation)
                            .conversationId(conversation.getId())
                            .inviterId(inviterResidentId)
                            .inviteeId(inviteeResidentId)
                            .status("PENDING")
                            .initialMessage(request.getInitialMessage())
                            .expiresAt(OffsetDateTime.now().plusDays(7)) // Set expiration to 7 days from now
                            .build();
                    newInvitation = invitationRepository.save(newInvitation);
                    log.info("Created new PENDING invitation ID: {}", newInvitation.getId());
                    return toResponse(newInvitation, accessToken);
                }
            }
            
            // If conversation status is BLOCKED or PENDING (after unblock), create new PENDING invitation
            // Users need to send invitation and get acceptance to chat again
            if ("BLOCKED".equals(conversation.getStatus()) || "PENDING".equals(conversation.getStatus())) {
                log.info("Conversation status is {} (after block/unblock). Checking for existing/reverse invitations.", conversation.getStatus());
                
                // First check for reverse invitation (invitee invited inviter) - if PENDING, inform user
                Optional<DirectInvitation> reverseInvitation = invitationRepository
                        .findByConversationAndParticipants(conversation.getId(), inviteeResidentId, inviterResidentId);
                
                if (reverseInvitation.isPresent() && "PENDING".equals(reverseInvitation.get().getStatus())) {
                    log.info("⚠️ Reverse invitation exists and is PENDING. User {} already sent invitation to {}. Cannot create new invitation.", 
                            reverseInvitation.get().getInviterId(), reverseInvitation.get().getInviteeId());
                    log.info("   Reverse invitation details - ID: {}, Status: {}, Inviter: {}, Invitee: {}", 
                            reverseInvitation.get().getId(), reverseInvitation.get().getStatus(), 
                            reverseInvitation.get().getInviterId(), reverseInvitation.get().getInviteeId());
                    
                    // Get inviter name for error message
                    String inviterName = residentInfoService.getResidentName(reverseInvitation.get().getInviterId(), accessToken);
                    String errorMessage = inviterName != null 
                        ? inviterName + " đã gửi lời mời cho bạn rồi. Vui lòng vào mục lời mời để xác nhận."
                        : "Người dùng này đã gửi lời mời cho bạn rồi. Vui lòng vào mục lời mời để xác nhận.";
                    
                    throw new RuntimeException(errorMessage);
                }
                
                // Check if there's an existing invitation from inviter to invitee
                DirectInvitation existingInv = invitationRepository
                        .findByConversationAndParticipants(conversation.getId(), inviterResidentId, inviteeResidentId)
                        .orElse(null);
                
                if (existingInv != null) {
                    // If existing invitation is PENDING, inform user they already sent invitation
                    if ("PENDING".equals(existingInv.getStatus())) {
                        log.info("⚠️ Existing invitation is PENDING. User {} already sent invitation to {}. Cannot create new invitation.", 
                                inviterResidentId, inviteeResidentId);
                        log.info("   Existing invitation details - ID: {}, Status: {}, Inviter: {}, Invitee: {}", 
                                existingInv.getId(), existingInv.getStatus(), 
                                existingInv.getInviterId(), existingInv.getInviteeId());
                        throw new RuntimeException("Bạn đã gửi lời mời rồi. Vui lòng đợi phản hồi từ người dùng.");
                    }
                    
                    // Update existing invitation to PENDING if it's not already (for DECLINED cases)
                    if (!"PENDING".equals(existingInv.getStatus())) {
                        existingInv.setStatus("PENDING");
                        existingInv.setRespondedAt(null);
                        existingInv.setInitialMessage(request.getInitialMessage());
                        existingInv = invitationRepository.save(existingInv);
                        log.info("Updated existing invitation to PENDING. ID: {}", existingInv.getId());
                    }
                    return toResponse(existingInv, accessToken);
                } else {
                    // Create new PENDING invitation
                    DirectInvitation newInvitation = DirectInvitation.builder()
                            .conversation(conversation)
                            .conversationId(conversation.getId())
                            .inviterId(inviterResidentId)
                            .inviteeId(inviteeResidentId)
                            .status("PENDING")
                            .initialMessage(request.getInitialMessage())
                            .expiresAt(OffsetDateTime.now().plusDays(7)) // Set expiration to 7 days from now
                            .build();
                    newInvitation = invitationRepository.save(newInvitation);
                    log.info("Created new PENDING invitation ID: {} for conversation with status {}", 
                            newInvitation.getId(), conversation.getStatus());
                    return toResponse(newInvitation, accessToken);
                }
            }
            // If conversation status is BLOCKED, reset to PENDING to allow new invitations
            if ("BLOCKED".equals(conversation.getStatus())) {
                log.info("Conversation status is BLOCKED, resetting to PENDING: {}", conversation.getId());
                conversation.setStatus("PENDING");
                conversation = conversationRepository.save(conversation);
            }
        } else {
            // Create new conversation
            conversation = Conversation.builder()
                    .participant1Id(participant1Id)
                    .participant2Id(participant2Id)
                    .status("PENDING")
                    .createdBy(inviterResidentId)
                    .build();
            conversation = conversationRepository.save(conversation);

            // Create participants
            participantRepository.save(com.QhomeBase.chatservice.model.ConversationParticipant.builder()
                    .conversation(conversation)
                    .conversationId(conversation.getId())
                    .residentId(participant1Id)
                    .build());
            participantRepository.save(com.QhomeBase.chatservice.model.ConversationParticipant.builder()
                    .conversation(conversation)
                    .conversationId(conversation.getId())
                    .residentId(participant2Id)
                    .build());
        }

        log.info("=== createInvitation ===");
        log.info("InviterId (userId from JWT): {}", inviterId);
        log.info("InviterResidentId (converted): {}", inviterResidentId);
        log.info("InviteeId from request: {}", request.getInviteeId());
        log.info("InviteeResidentId (used): {}", inviteeResidentId);
        log.info("Conversation ID: {}", conversation.getId());
        log.info("Conversation Status: {}", conversation.getStatus());
        
        // Check if invitation already exists (any status) to avoid unique constraint violation
        // This checks for invitation from inviterResidentId to inviteeResidentId
        DirectInvitation existingInvitation = invitationRepository
                .findByConversationAndParticipants(conversation.getId(), inviterResidentId, inviteeResidentId)
                .orElse(null);
        
        // Also check for reverse invitation (invitee invited inviter)
        Optional<DirectInvitation> reverseInvitation = invitationRepository
                .findByConversationAndParticipants(conversation.getId(), inviteeResidentId, inviterResidentId);
        
        if (reverseInvitation.isPresent()) {
            log.info("Found reverse invitation ID: {}, Status: {}, Inviter: {}, Invitee: {}", 
                    reverseInvitation.get().getId(), 
                    reverseInvitation.get().getStatus(),
                    reverseInvitation.get().getInviterId(),
                    reverseInvitation.get().getInviteeId());
        } else {
            log.info("No reverse invitation found");
        }
        
        DirectInvitation invitation;
        if (existingInvitation != null) {
            log.info("Found existing invitation ID: {}, Status: {}", existingInvitation.getId(), existingInvitation.getStatus());
            
            // If invitation exists
            if ("PENDING".equals(existingInvitation.getStatus())) {
                // Check if there's a reverse invitation PENDING - if so, both users want to chat
                // BUT: Only auto-accept if users are actually friends (friendship is active)
                // After block/unblock, friendship is inactive, so don't auto-accept
                boolean areFriends = friendshipService.areFriends(inviterResidentId, inviteeResidentId);
                
                if (reverseInvitation.isPresent() && "PENDING".equals(reverseInvitation.get().getStatus()) && areFriends) {
                    log.info("Found mutual invitations (both PENDING) and users are friends. Auto-accepting both invitations and creating friendship.");
                    
                    // Auto-accept existing invitation (A->B)
                    existingInvitation.setStatus("ACCEPTED");
                    existingInvitation.setRespondedAt(OffsetDateTime.now());
                    invitation = invitationRepository.save(existingInvitation);
                    log.info("Auto-accepted existing invitation ID: {}", invitation.getId());
                    
                    // Auto-accept reverse invitation (B->A)
                    DirectInvitation reverseInv = reverseInvitation.get();
                    reverseInv.setStatus("ACCEPTED");
                    reverseInv.setRespondedAt(OffsetDateTime.now());
                    invitationRepository.save(reverseInv);
                    log.info("Auto-accepted reverse invitation ID: {}", reverseInv.getId());
                    
                    // Activate conversation
                    conversation.setStatus("ACTIVE");
                    conversation = conversationRepository.save(conversation);
                    log.info("Activated conversation ID: {}", conversation.getId());
                    
                    // Create or activate friendship between both users
                    friendshipService.createOrActivateFriendship(inviterResidentId, inviteeResidentId);
                    log.info("Friendship created/activated between {} and {} (mutual invitations - both PENDING)", inviterResidentId, inviteeResidentId);
                    
                    // Notify both users that conversation is now active
                    notificationService.notifyDirectInvitationAccepted(
                            reverseInv.getInviterId(),
                            conversation.getId(),
                            toResponse(reverseInv, accessToken));
                    notificationService.notifyDirectInvitationAccepted(
                            invitation.getInviterId(),
                            conversation.getId(),
                            toResponse(invitation, accessToken));
                } else if (reverseInvitation.isPresent() && "PENDING".equals(reverseInvitation.get().getStatus()) && !areFriends) {
                    // Mutual invitations but users are NOT friends (after block/unblock)
                    // Don't auto-accept - require explicit acceptance
                    log.info("Found mutual invitations (both PENDING) but users are NOT friends (friendship inactive). Not auto-accepting - requires explicit acceptance.");
                    invitation = existingInvitation;
                } else {
                    // PENDING - user already sent invitation, inform them
                    log.info("Invitation already exists and is pending: {}. User {} already sent invitation to {}.", 
                            existingInvitation.getId(), inviterResidentId, inviteeResidentId);
                    // Throw exception to inform user they already sent invitation
                    throw new RuntimeException("Bạn đã gửi lời mời rồi. Vui lòng đợi phản hồi từ người dùng.");
                }
            } else if ("ACCEPTED".equals(existingInvitation.getStatus())) {
                // Already accepted - check conversation status
                if ("ACTIVE".equals(conversation.getStatus())) {
                    // Conversation is ACTIVE - invitation already accepted
                    log.info("Invitation already accepted and conversation is ACTIVE. Invitation ID: {}", existingInvitation.getId());
                    throw new RuntimeException("Conversation already exists and is active");
                } else {
                    // Data inconsistency: invitation ACCEPTED but conversation not ACTIVE
                    // This can happen if conversation was DELETED/hidden and then reset to PENDING
                    // Check if there's a reverse invitation PENDING - if so, auto-accept it and activate conversation
                    if (reverseInvitation.isPresent() && "PENDING".equals(reverseInvitation.get().getStatus())) {
                        log.info("Invitation ACCEPTED but conversation PENDING. Found reverse invitation PENDING. Auto-accepting reverse invitation and activating conversation.");
                        
                        // Auto-accept reverse invitation
                        DirectInvitation reverseInv = reverseInvitation.get();
                        reverseInv.setStatus("ACCEPTED");
                        reverseInv.setRespondedAt(OffsetDateTime.now());
                        invitationRepository.save(reverseInv);
                        log.info("Auto-accepted reverse invitation ID: {}", reverseInv.getId());
                        
                        // Activate conversation
                        conversation.setStatus("ACTIVE");
                        conversation = conversationRepository.save(conversation);
                        log.info("Activated conversation ID: {}", conversation.getId());
                        
                        // Create or activate friendship between both users
                        friendshipService.createOrActivateFriendship(inviterResidentId, inviteeResidentId);
                        log.info("Friendship created/activated between {} and {} (invitation accepted with reverse pending)", inviterResidentId, inviteeResidentId);
                        
                        // Notify both users that conversation is now active
                        notificationService.notifyDirectInvitationAccepted(
                                reverseInv.getInviterId(),
                                conversation.getId(),
                                toResponse(reverseInv, accessToken));
                        notificationService.notifyDirectInvitationAccepted(
                                existingInvitation.getInviterId(),
                                conversation.getId(),
                                toResponse(existingInvitation, accessToken));
                        
                        invitation = existingInvitation;
                    } else {
                        // No reverse invitation - reset invitation to PENDING so invitee can see and accept it
                        // This handles the case where invitation was ACCEPTED but conversation was reset to PENDING
                        log.info("Invitation ACCEPTED but conversation PENDING and no reverse invitation. Resetting invitation to PENDING so invitee can accept. Invitation ID: {}", existingInvitation.getId());
                        existingInvitation.setStatus("PENDING");
                        existingInvitation.setRespondedAt(null);
                        existingInvitation.setInitialMessage(request.getInitialMessage());
                        invitation = invitationRepository.save(existingInvitation);
                        log.info("Reset invitation from ACCEPTED to PENDING. ID: {}", invitation.getId());
                    }
                }
            } else {
                // DECLINED - update to PENDING
                log.info("Invitation exists with status {}, updating to PENDING: {}", 
                        existingInvitation.getStatus(), existingInvitation.getId());
                existingInvitation.setStatus("PENDING");
                existingInvitation.setInitialMessage(request.getInitialMessage());
                existingInvitation.setRespondedAt(null);
                invitation = invitationRepository.save(existingInvitation);
                log.info("Updated invitation from {} to PENDING. ID: {}", existingInvitation.getStatus(), invitation.getId());
            }
        } else {
            // Create new invitation
            // Check if reverse invitation exists and is PENDING - if so, inform user that other person already sent invitation
            if (reverseInvitation.isPresent() && "PENDING".equals(reverseInvitation.get().getStatus())) {
                log.info("⚠️ Reverse invitation exists and is PENDING. User {} already sent invitation to {}. Cannot create new invitation.", 
                        reverseInvitation.get().getInviterId(), reverseInvitation.get().getInviteeId());
                log.info("   Reverse invitation details - ID: {}, Status: {}, Inviter: {}, Invitee: {}", 
                        reverseInvitation.get().getId(), reverseInvitation.get().getStatus(), 
                        reverseInvitation.get().getInviterId(), reverseInvitation.get().getInviteeId());
                
                // Get inviter name for error message
                String inviterName = residentInfoService.getResidentName(reverseInvitation.get().getInviterId(), accessToken);
                String errorMessage = inviterName != null 
                    ? inviterName + " đã gửi lời mời cho bạn rồi. Vui lòng vào mục lời mời để xác nhận."
                    : "Người dùng này đã gửi lời mời cho bạn rồi. Vui lòng vào mục lời mời để xác nhận.";
                
                throw new RuntimeException(errorMessage);
            } else {
                // Create new invitation normally
                log.info("📝 Creating new invitation - Inviter: {}, Invitee: {}, Conversation: {}", 
                        inviterResidentId, inviteeResidentId, conversation.getId());
                
                invitation = DirectInvitation.builder()
                        .conversation(conversation)
                        .conversationId(conversation.getId())
                        .inviterId(inviterResidentId)
                        .inviteeId(inviteeResidentId)
                        .status("PENDING")
                        .initialMessage(request.getInitialMessage())
                        .expiresAt(OffsetDateTime.now().plusDays(7)) // Set expiration to 7 days from now
                        .build();
                invitation = invitationRepository.save(invitation);
                
                log.info("✅ Created new invitation ID: {}, Status: PENDING, Inviter: {}, Invitee: {}, CreatedAt: {}", 
                        invitation.getId(), invitation.getInviterId(), invitation.getInviteeId(), 
                        invitation.getCreatedAt());
                
                // Verify invitation was saved correctly
                Optional<DirectInvitation> savedInvitation = invitationRepository.findById(invitation.getId());
                if (savedInvitation.isPresent()) {
                    log.info("✅ Verified invitation saved correctly - ID: {}, Status: {}", 
                            savedInvitation.get().getId(), savedInvitation.get().getStatus());
                } else {
                    log.error("❌ ERROR: Invitation was not saved correctly! ID: {}", invitation.getId());
                }
            }
        }
        
        log.info("✅ Final invitation - ID: {}, Status: {}, Inviter: {}, Invitee: {}", 
                invitation.getId(), invitation.getStatus(), invitation.getInviterId(), invitation.getInviteeId());

        // If initial message provided, create it as a message
        if (request.getInitialMessage() != null && !request.getInitialMessage().trim().isEmpty()) {
            DirectMessage initialMessage = DirectMessage.builder()
                    .conversation(conversation)
                    .conversationId(conversation.getId())
                    .senderId(inviterResidentId)
                    .content(request.getInitialMessage())
                    .messageType("TEXT")
                    .build();
            messageRepository.save(initialMessage);
        }

        // Notify invitee via WebSocket and FCM
        notificationService.notifyDirectInvitation(inviteeResidentId, toResponse(invitation, accessToken));
        fcmPushService.sendDirectInvitationNotification(inviteeResidentId, inviterResidentId, conversation.getId());

        return toResponse(invitation, accessToken);
    }

    /**
     * Accept invitation
     */
    @Transactional
    public DirectInvitationResponse acceptInvitation(UUID invitationId, UUID userId) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            throw new RuntimeException("Resident not found for user: " + userId);
        }

        DirectInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new RuntimeException("Invitation not found"));

        // Verify invitee
        if (!invitation.getInviteeId().equals(residentId)) {
            throw new RuntimeException("This invitation is not for you");
        }

        // Check if already responded
        if (!"PENDING".equals(invitation.getStatus())) {
            throw new RuntimeException("Invitation has already been responded to");
        }

        // Update invitation
        invitation.setStatus("ACCEPTED");
        invitation.setRespondedAt(OffsetDateTime.now());
        invitation = invitationRepository.save(invitation);

        // Activate conversation
        Conversation conversation = invitation.getConversation();
        conversation.setStatus("ACTIVE");
        conversationRepository.save(conversation);

        // Unhide conversation for both participants when invitation is accepted
        List<ConversationParticipant> participants = participantRepository.findByConversationId(conversation.getId());
        for (ConversationParticipant participant : participants) {
            if (Boolean.TRUE.equals(participant.getIsHidden())) {
                participant.setIsHidden(false);
                participant.setHiddenAt(null);
                participantRepository.save(participant);
                log.info("Unhidden conversation {} for participant {} when invitation accepted", conversation.getId(), participant.getResidentId());
            }
        }

        // Check if there's a reverse invitation (invitee invited inviter) and auto-accept it
        UUID inviterId = invitation.getInviterId();
        UUID inviteeId = invitation.getInviteeId();
        Optional<DirectInvitation> reverseInvitation = invitationRepository
                .findByConversationAndParticipants(conversation.getId(), inviteeId, inviterId);
        
        if (reverseInvitation.isPresent() && "PENDING".equals(reverseInvitation.get().getStatus())) {
            log.info("Found reverse invitation, auto-accepting: {}", reverseInvitation.get().getId());
            DirectInvitation reverseInv = reverseInvitation.get();
            reverseInv.setStatus("ACCEPTED");
            reverseInv.setRespondedAt(OffsetDateTime.now());
            invitationRepository.save(reverseInv);
            log.info("Auto-accepted reverse invitation: {}", reverseInv.getId());
        }

        // Create or activate friendship between inviter and invitee
        friendshipService.createOrActivateFriendship(inviterId, inviteeId);
        log.info("Friendship created/activated between {} and {}", inviterId, inviteeId);

        // Notify inviter via WebSocket
        notificationService.notifyDirectInvitationAccepted(
                invitation.getInviterId(),
                conversation.getId(),
                toResponse(invitation, accessToken));

        return toResponse(invitation, accessToken);
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

        DirectInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new RuntimeException("Invitation not found"));

        // Verify invitee
        if (!invitation.getInviteeId().equals(residentId)) {
            throw new RuntimeException("This invitation is not for you");
        }

        // Check if already responded
        if (!"PENDING".equals(invitation.getStatus())) {
            throw new RuntimeException("Invitation has already been responded to");
        }

        // Delete invitation when declined - this resets state to "chưa gửi lời mời"
        // After decline, users are in "chưa gửi lời mời" state and can send new invitation
        UUID inviterId = invitation.getInviterId();
        UUID inviteeId = invitation.getInviteeId();
        UUID deletedInvitationId = invitation.getId();
        
        Conversation conversation = invitation.getConversation();
        
        // Delete the invitation
        invitationRepository.delete(invitation);
        log.info("Deleted invitation {} after decline ({} -> {}). Users are now in 'chưa gửi lời mời' state.", 
                deletedInvitationId, inviterId, inviteeId);

        // Don't close conversation - keep it ACTIVE or PENDING so users can still see it
        // Users can send invitation again later if they want
        // After decline, invitation is deleted, so users are in "chưa gửi lời mời" state
        log.info("Conversation {} status remains {} (not changed to CLOSED). Users can still see conversation and send invitation again.", 
                conversation.getId(), conversation.getStatus());

        // Notify inviter via WebSocket (use saved variables since invitation is deleted)
        notificationService.notifyDirectInvitationDeclined(
                inviterId,
                conversation.getId());
    }

    /**
     * Get pending invitations for a user
     * Always returns a list (may be empty) - screen should always be visible like group invitations
     * Returns all PENDING invitations (invitations no longer expire, only accept/decline changes status)
     */
    public List<DirectInvitationResponse> getPendingInvitations(UUID userId) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        
        log.info("=== getPendingInvitations ===");
        log.info("UserId from JWT: {}", userId);
        log.info("ResidentId converted: {}", residentId);
        
        if (residentId == null) {
            log.error("Resident not found for user: {}", userId);
            // Return empty list instead of throwing exception - allows screen to always be visible
            log.warn("Returning empty list for user {} (resident not found) - screen should still be visible", userId);
            return new ArrayList<>();
        }

        // Get all PENDING invitations (invitations no longer expire)
        List<DirectInvitation> invitations = invitationRepository
                .findPendingInvitationsByInviteeId(residentId);
        
        log.info("Found {} pending invitations for residentId: {}", invitations.size(), residentId);
        
        // Log details of each invitation for debugging
        for (DirectInvitation inv : invitations) {
            log.info("  - Invitation ID: {}, Status: {}, Inviter: {}, Invitee: {}, ExpiresAt: {}, CreatedAt: {}", 
                    inv.getId(), inv.getStatus(), inv.getInviterId(), inv.getInviteeId(), inv.getExpiresAt(), inv.getCreatedAt());
        }
        
        // Also check if there are any invitations where this user is the inviter (for debugging)
        List<DirectInvitation> sentInvitations = invitationRepository.findAll().stream()
                .filter(inv -> inv.getInviterId().equals(residentId) && "PENDING".equals(inv.getStatus()))
                .collect(Collectors.toList());
        log.info("Found {} sent PENDING invitations for residentId: {} (as inviter)", sentInvitations.size(), residentId);
        for (DirectInvitation inv : sentInvitations) {
            log.info("  - Sent Invitation ID: {}, Status: {}, Inviter: {}, Invitee: {}, ExpiresAt: {}", 
                    inv.getId(), inv.getStatus(), inv.getInviterId(), inv.getInviteeId(), inv.getExpiresAt());
        }
        
        // Always return a list (may be empty) - this allows the screen to always be visible
        // Frontend should display the screen regardless of whether list is empty or not
        return invitations.stream()
                .map(inv -> toResponse(inv, accessToken))
                .collect(Collectors.toList());
    }

    /**
     * Count pending invitations for a user
     */
    public Long countPendingInvitations(UUID userId) {
        String accessToken = getCurrentAccessToken();
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        
        log.info("=== countPendingInvitations (DIRECT Invitations) ===");
        log.info("UserId from JWT: {}", userId);
        log.info("ResidentId converted: {}", residentId);
        
        if (residentId == null) {
            log.error("Resident not found for user: {}", userId);
            return 0L;
        }

        Long count = invitationRepository.countPendingInvitationsByInviteeId(residentId);
        log.info("Total pending DIRECT invitations found for residentId {}: {}", residentId, count);
        
        return count;
    }

    private DirectInvitationResponse toResponse(DirectInvitation invitation, String accessToken) {
        // Get resident names
        String inviterName = residentInfoService.getResidentName(invitation.getInviterId(), accessToken);
        String inviteeName = residentInfoService.getResidentName(invitation.getInviteeId(), accessToken);

        return DirectInvitationResponse.builder()
                .id(invitation.getId())
                .conversationId(invitation.getConversationId())
                .inviterId(invitation.getInviterId())
                .inviterName(inviterName)
                .inviteeId(invitation.getInviteeId())
                .inviteeName(inviteeName)
                .status(invitation.getStatus())
                .initialMessage(invitation.getInitialMessage())
                .createdAt(invitation.getCreatedAt())
                .expiresAt(invitation.getExpiresAt())
                .respondedAt(invitation.getRespondedAt())
                .build();
    }
}

