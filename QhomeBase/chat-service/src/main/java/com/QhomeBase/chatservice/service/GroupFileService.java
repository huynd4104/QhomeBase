package com.QhomeBase.chatservice.service;

import com.QhomeBase.chatservice.dto.GroupFileResponse;
import com.QhomeBase.chatservice.model.Group;
import com.QhomeBase.chatservice.model.GroupFile;
import com.QhomeBase.chatservice.model.GroupMember;
import com.QhomeBase.chatservice.model.Message;
import com.QhomeBase.chatservice.repository.GroupFileRepository;
import com.QhomeBase.chatservice.repository.GroupMemberRepository;
import com.QhomeBase.chatservice.repository.GroupRepository;
import com.QhomeBase.chatservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.QhomeBase.chatservice.dto.GroupFilePagedResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupFileService {

    private final GroupFileRepository groupFileRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ResidentInfoService residentInfoService;

    /**
     * Get all files in a group with pagination
     */
    @Transactional(readOnly = true)
    public GroupFilePagedResponse getGroupFiles(UUID groupId, UUID userId, int page, int size) {
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

        Pageable pageable = PageRequest.of(page, size);
        Page<GroupFile> files = groupFileRepository.findByGroupIdOrderByCreatedAtDesc(groupId, pageable);

        // Convert to GroupFilePagedResponse
        return GroupFilePagedResponse.builder()
                .content(files.getContent().stream()
                        .map(this::toGroupFileResponse)
                        .collect(java.util.stream.Collectors.toList()))
                .currentPage(files.getNumber())
                .pageSize(files.getSize())
                .totalElements(files.getTotalElements())
                .totalPages(files.getTotalPages())
                .hasNext(files.hasNext())
                .hasPrevious(files.hasPrevious())
                .isFirst(files.isFirst())
                .isLast(files.isLast())
                .build();
    }

    /**
     * Save file metadata when a file message is sent
     */
    @Transactional
    public GroupFile saveFileMetadata(Message message) {
        // Only save for FILE, IMAGE, and AUDIO message types
        if (!"FILE".equals(message.getMessageType()) && 
            !"IMAGE".equals(message.getMessageType()) && 
            !"AUDIO".equals(message.getMessageType())) {
            return null;
        }

        // Check if file metadata already exists for this message
        if (groupFileRepository.existsByMessageId(message.getId())) {
            return groupFileRepository.findByMessageId(message.getId());
        }

        // Determine file URL based on message type
        String fileUrl = null;
        if ("IMAGE".equals(message.getMessageType())) {
            fileUrl = message.getImageUrl();
        } else if ("FILE".equals(message.getMessageType()) || "AUDIO".equals(message.getMessageType())) {
            fileUrl = message.getFileUrl();
        }

        if (fileUrl == null || fileUrl.isEmpty()) {
            return null;
        }

        // Determine fileType from messageType and mimeType
        String fileType = "DOCUMENT";
        if ("IMAGE".equals(message.getMessageType())) {
            fileType = "IMAGE";
        } else if ("AUDIO".equals(message.getMessageType())) {
            fileType = "AUDIO";
        } else if ("VIDEO".equals(message.getMessageType())) {
            fileType = "VIDEO";
        }
        
        GroupFile groupFile = GroupFile.builder()
                .group(message.getGroup())
                .groupId(message.getGroupId())
                .message(message)
                .messageId(message.getId())
                .senderId(message.getSenderId())
                .fileName(message.getFileName() != null ? message.getFileName() : "file")
                .fileSize(message.getFileSize() != null ? message.getFileSize() : 0L)
                .fileType(fileType)
                .mimeType(message.getMimeType())
                .fileUrl(fileUrl)
                .build();

        return groupFileRepository.save(groupFile);
    }

    /**
     * Convert GroupFile entity to GroupFileResponse DTO
     */
    private GroupFileResponse toGroupFileResponse(GroupFile groupFile) {
        String accessToken = getCurrentAccessToken();
        
        // Get sender info
        Map<String, Object> senderInfo = residentInfoService.getResidentInfo(groupFile.getSenderId());
        String senderName = senderInfo != null ? (String) senderInfo.get("fullName") : "Người dùng";
        String senderAvatarUrl = senderInfo != null ? (String) senderInfo.get("avatarUrl") : null;

        // Determine mimeType: prefer mimeType field, fallback to fileType if it looks like a mime type
        String mimeType = groupFile.getMimeType();
        if (mimeType == null || mimeType.isEmpty()) {
            // If fileType is a mime type (contains '/'), use it as mimeType
            String fileType = groupFile.getFileType();
            if (fileType != null && fileType.contains("/")) {
                mimeType = fileType;
            } else {
                // Otherwise, try to infer from fileType enum
                if ("IMAGE".equals(fileType)) {
                    // Try to infer from file name extension
                    String fileName = groupFile.getFileName();
                    if (fileName != null) {
                        String lowerFileName = fileName.toLowerCase();
                        if (lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg")) {
                            mimeType = "image/jpeg";
                        } else if (lowerFileName.endsWith(".png")) {
                            mimeType = "image/png";
                        } else if (lowerFileName.endsWith(".gif")) {
                            mimeType = "image/gif";
                        } else if (lowerFileName.endsWith(".webp")) {
                            mimeType = "image/webp";
                        } else if (lowerFileName.endsWith(".heic")) {
                            mimeType = "image/heic";
                        } else {
                            mimeType = "image/jpeg"; // Default for IMAGE type
                        }
                    } else {
                        mimeType = "image/jpeg"; // Default for IMAGE type
                    }
                } else if ("AUDIO".equals(fileType)) {
                    mimeType = "audio/mpeg"; // Default for AUDIO type
                } else if ("VIDEO".equals(fileType)) {
                    mimeType = "video/mp4"; // Default for VIDEO type
                } else {
                    mimeType = "application/octet-stream"; // Default for DOCUMENT type
                }
            }
        }
        
        return GroupFileResponse.builder()
                .id(groupFile.getId())
                .groupId(groupFile.getGroupId())
                .messageId(groupFile.getMessageId())
                .senderId(groupFile.getSenderId())
                .senderName(senderName)
                .senderAvatar(senderAvatarUrl)
                .fileName(groupFile.getFileName())
                .fileSize(groupFile.getFileSize())
                .fileType(groupFile.getFileType())
                .mimeType(mimeType)
                .fileUrl(groupFile.getFileUrl())
                .createdAt(groupFile.getCreatedAt())
                .build();
    }

    /**
     * Find files uploaded by a user in a group from a specific time onwards
     */
    @Transactional(readOnly = true)
    public List<GroupFile> findFilesByGroupIdAndSenderIdFromTime(
            UUID groupId, UUID senderId, java.time.OffsetDateTime fromTime) {
        return groupFileRepository.findFilesByGroupIdAndSenderIdFromTime(groupId, senderId, fromTime);
    }

    /**
     * Delete file metadata by ID
     */
    @Transactional
    public void deleteFileMetadata(UUID fileId) {
        groupFileRepository.deleteById(fileId);
        log.info("Deleted file metadata: {}", fileId);
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

