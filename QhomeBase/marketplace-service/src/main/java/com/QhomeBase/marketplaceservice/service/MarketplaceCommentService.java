package com.QhomeBase.marketplaceservice.service;

import com.QhomeBase.marketplaceservice.model.MarketplaceComment;
import com.QhomeBase.marketplaceservice.model.MarketplacePost;
import com.QhomeBase.marketplaceservice.repository.MarketplaceCommentRepository;
import com.QhomeBase.marketplaceservice.repository.MarketplacePostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import org.springframework.lang.NonNull;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketplaceCommentService {

    private final MarketplaceCommentRepository commentRepository;
    private final MarketplacePostRepository postRepository;
    private final MarketplaceNotificationService notificationService;
    private final ChatServiceClient chatServiceClient;
    private final ResidentInfoService residentInfoService;

    /**
     * Get comments for a post (all comments, no pagination)
     */
    @Transactional(readOnly = true)
    public List<MarketplaceComment> getComments(UUID postId, UUID currentResidentId, String accessToken) {
        List<MarketplaceComment> comments = commentRepository.findByPostIdAndParentCommentIsNullOrderByCreatedAtAsc(postId);
        // Force initialization of all nested replies recursively within transaction
        comments.forEach(comment -> initializeReplies(comment, currentResidentId, accessToken));
        
        // Filter comments based on block relationship
        if (currentResidentId != null && accessToken != null && !accessToken.isEmpty()) {
            comments = filterBlockedComments(comments, currentResidentId, accessToken);
        }
        
        return comments;
    }

    /**
     * Get paginated comments for a post
     */
    @Transactional(readOnly = true)
    public Page<MarketplaceComment> getComments(UUID postId, UUID currentResidentId, String accessToken, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<MarketplaceComment> commentsPage = commentRepository.findByPostIdAndParentCommentIsNullOrderByCreatedAtAsc(postId, pageable);
        // Force initialization of all nested replies recursively within transaction
        commentsPage.getContent().forEach(comment -> initializeReplies(comment, currentResidentId, accessToken));
        
        // Filter comments based on block relationship
        if (currentResidentId != null && accessToken != null && !accessToken.isEmpty()) {
            List<MarketplaceComment> filteredComments = filterBlockedComments(commentsPage.getContent(), currentResidentId, accessToken);
            
            // Create new Page with filtered content
            if (filteredComments.size() != commentsPage.getContent().size()) {
                return new org.springframework.data.domain.PageImpl<>(
                        filteredComments,
                        pageable,
                        commentsPage.getTotalElements() - (commentsPage.getContent().size() - filteredComments.size())
                );
            }
        }
        
        return commentsPage;
    }

    /**
     * Recursively initialize all nested replies to prevent LazyInitializationException
     * Also filters out deleted replies
     */
    private void initializeReplies(MarketplaceComment comment, UUID currentResidentId, String accessToken) {
        if (comment.getReplies() != null) {
            // Force initialization of the replies collection
            Hibernate.initialize(comment.getReplies());
            // Filter out deleted replies and recursively initialize remaining ones
            comment.getReplies().removeIf(MarketplaceComment::isDeleted);
            comment.getReplies().forEach(reply -> initializeReplies(reply, currentResidentId, accessToken));
        }
    }

    /**
     * Filter comments based on block relationship
     * Rules:
     * - If root comment author is blocked → hide entire comment tree (comment + all replies)
     * - If child comment author is blocked → hide only that child comment (keep replies)
     */
    private List<MarketplaceComment> filterBlockedComments(List<MarketplaceComment> comments, UUID currentResidentId, String accessToken) {
        try {
            // Get blocked users
            List<UUID> blockedUserIds = chatServiceClient.getBlockedUserIds(accessToken);
            List<UUID> blockedByUserIds = chatServiceClient.getBlockedByUserIds(accessToken);
            java.util.Set<UUID> allBlockedUserIds = new java.util.HashSet<>();
            allBlockedUserIds.addAll(blockedUserIds);
            allBlockedUserIds.addAll(blockedByUserIds);
            
            if (allBlockedUserIds.isEmpty()) {
                return comments;
            }
            
            log.info("🔍 [MarketplaceCommentService] Filtering comments - blocked users: {}", allBlockedUserIds);
            
            return comments.stream()
                    .filter(comment -> {
                        // Check if root comment author is blocked
                        if (allBlockedUserIds.contains(comment.getResidentId())) {
                            log.info("🚫 [MarketplaceCommentService] Filtering root comment {} - author {} is blocked", 
                                    comment.getId(), comment.getResidentId());
                            return false; // Hide entire comment tree
                        }
                        
                        // Filter child comments (replies) - if child author is blocked, hide only that child
                        filterBlockedReplies(comment, allBlockedUserIds);
                        
                        return true;
                    })
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            log.error("❌ [MarketplaceCommentService] Error filtering blocked comments: {}", e.getMessage(), e);
            return comments; // Return unfiltered comments on error
        }
    }

    /**
     * Recursively filter blocked replies
     * If a child comment author is blocked → remove only that child comment, keep its replies
     */
    private void filterBlockedReplies(MarketplaceComment comment, java.util.Set<UUID> blockedUserIds) {
        if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
            List<MarketplaceComment> filteredReplies = new java.util.ArrayList<>();
            
            for (MarketplaceComment reply : comment.getReplies()) {
                // Check if reply author is blocked
                if (blockedUserIds.contains(reply.getResidentId())) {
                    log.info("🚫 [MarketplaceCommentService] Filtering child comment {} - author {} is blocked", 
                            reply.getId(), reply.getResidentId());
                    
                    // If this reply has children, move them to parent comment
                    if (reply.getReplies() != null && !reply.getReplies().isEmpty()) {
                        log.info("🔍 [MarketplaceCommentService] Moving {} replies from blocked comment {} to parent {}", 
                                reply.getReplies().size(), reply.getId(), comment.getId());
                        
                        // Recursively process replies before moving
                        for (MarketplaceComment childReply : reply.getReplies()) {
                            filterBlockedReplies(childReply, blockedUserIds);
                            filteredReplies.add(childReply);
                            // Update parent reference
                            childReply.setParentComment(comment);
                        }
                    }
                    // Don't add the blocked reply itself
                } else {
                    // Recursively filter replies of this reply
                    filterBlockedReplies(reply, blockedUserIds);
                    filteredReplies.add(reply);
                }
            }
            
            // Update replies list
            comment.setReplies(filteredReplies);
        }
    }

    /**
     * Add comment
     */
    @CacheEvict(value = {"postDetails"}, allEntries = true)
    @Transactional
    public MarketplaceComment addComment(UUID postId, UUID residentId, String content, UUID parentCommentId, String imageUrl, String videoUrl, UUID currentResidentId, String accessToken) {
        log.info("Adding comment to post: {} by user: {}", postId, residentId);

        // Validate: content, imageUrl, or videoUrl must be provided
        String trimmedContent = content != null ? content.trim() : "";
        boolean hasContent = !trimmedContent.isEmpty();
        boolean hasImage = imageUrl != null && !imageUrl.trim().isEmpty();
        boolean hasVideo = videoUrl != null && !videoUrl.trim().isEmpty();
        
        if (!hasContent && !hasImage && !hasVideo) {
            throw new IllegalArgumentException("Comment must have content, image, or video");
        }

        MarketplacePost post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found: " + postId));
        
        // Check if current user is blocked from commenting on this post
        if (currentResidentId != null && accessToken != null && !accessToken.isEmpty()) {
            try {
                List<UUID> blockedUserIds = chatServiceClient.getBlockedUserIds(accessToken);
                List<UUID> blockedByUserIds = chatServiceClient.getBlockedByUserIds(accessToken);
                java.util.Set<UUID> allBlockedUserIds = new java.util.HashSet<>();
                allBlockedUserIds.addAll(blockedUserIds);
                allBlockedUserIds.addAll(blockedByUserIds);
                
                // Check if post author is blocked (bidirectional)
                if (allBlockedUserIds.contains(post.getResidentId())) {
                    log.info("🚫 [MarketplaceCommentService] Cannot comment on post {} - post author {} is blocked", 
                            postId, post.getResidentId());
                    throw new RuntimeException("Cannot comment on this post");
                }
                
                // If replying to a comment, check if comment author is blocked
                if (parentCommentId != null) {
                    MarketplaceComment parentComment = commentRepository.findById(parentCommentId)
                            .orElseThrow(() -> new RuntimeException("Parent comment not found: " + parentCommentId));
                    
                    if (allBlockedUserIds.contains(parentComment.getResidentId())) {
                        log.info("🚫 [MarketplaceCommentService] Cannot reply to comment {} - comment author {} is blocked", 
                                parentCommentId, parentComment.getResidentId());
                        throw new RuntimeException("Cannot reply to this comment");
                    }
                }
            } catch (RuntimeException e) {
                // Re-throw if it's our custom exception
                if (e.getMessage() != null && (e.getMessage().contains("Cannot comment") || e.getMessage().contains("Cannot reply"))) {
                    throw e;
                }
                log.error("❌ [MarketplaceCommentService] Error checking blocked users for comment: {}", e.getMessage(), e);
                // Continue if error occurs (fail open)
            } catch (Exception e) {
                log.error("❌ [MarketplaceCommentService] Error checking blocked users for comment: {}", e.getMessage(), e);
                // Continue if error occurs (fail open)
            }
        }

        MarketplaceComment comment = MarketplaceComment.builder()
                .post(post)
                .residentId(residentId)
                .content(hasContent ? trimmedContent : null) // Set to null if empty
                .imageUrl(imageUrl)
                .videoUrl(videoUrl)
                .build();

        if (parentCommentId != null) {
            MarketplaceComment parent = commentRepository.findById(parentCommentId)
                    .orElseThrow(() -> new RuntimeException("Parent comment not found: " + parentCommentId));
            comment.setParentComment(parent);
        }

        MarketplaceComment saved = commentRepository.save(comment);
        post.incrementCommentCount();
        postRepository.save(post);

        // ✅ FIX LazyInitializationException: Initialize replies collection within transaction
        saved.getReplies().size(); // Force initialization

        // Get post owner residentId for notification
        UUID postOwnerResidentId = post.getResidentId();
        
        // Get comment author name for notification - fetch from resident info
        String authorName = "User"; // Default fallback
        if (accessToken != null && !accessToken.isEmpty()) {
            try {
                var residentInfo = residentInfoService.getResidentInfo(residentId, accessToken);
                if (residentInfo != null && residentInfo.getName() != null && !residentInfo.getName().trim().isEmpty()) {
                    authorName = residentInfo.getName();
                    log.debug("✅ [MarketplaceCommentService] Fetched author name for comment: {} -> {}", residentId, authorName);
                } else {
                    log.warn("⚠️ [MarketplaceCommentService] Resident info not found or name is empty for residentId: {}", residentId);
                }
            } catch (Exception e) {
                log.error("❌ [MarketplaceCommentService] Error fetching resident info for notification (residentId: {}): {}", 
                        residentId, e.getMessage());
                // Continue with default "User" name
            }
        }
        
        // Get parentCommentId if this is a reply
        // Notify post owner and viewers (both WebSocket and FCM push)
        notificationService.notifyNewComment(postId, saved.getId(), residentId, authorName, postOwnerResidentId, parentCommentId);

        return saved;
    }

    /**
     * Update comment
     */
    @Transactional
    public MarketplaceComment updateComment(@NonNull UUID commentId, @NonNull UUID residentId, String content, String imageUrl, String videoUrl) {
        log.info("Updating comment: {} by user: {}", commentId, residentId);

        MarketplaceComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found: " + commentId));

        if (!comment.getResidentId().equals(residentId)) {
            throw new RuntimeException("Not authorized to update this comment");
        }

        // Validate: content, imageUrl, or videoUrl must be provided
        String trimmedContent = content != null ? content.trim() : "";
        boolean hasContent = !trimmedContent.isEmpty();
        boolean hasImage = imageUrl != null && !imageUrl.trim().isEmpty();
        boolean hasVideo = videoUrl != null && !videoUrl.trim().isEmpty();
        
        if (!hasContent && !hasImage && !hasVideo) {
            throw new IllegalArgumentException("Comment must have content, image, or video");
        }

        // Update content (set to null if empty)
        comment.setContent(hasContent ? trimmedContent : null);
        
        // Update imageUrl
        comment.setImageUrl(hasImage ? imageUrl.trim() : null);
        
        // Update videoUrl
        comment.setVideoUrl(hasVideo ? videoUrl.trim() : null);
        
        MarketplaceComment saved = commentRepository.save(comment);
        
        // ✅ FIX LazyInitializationException: Initialize replies collection within transaction
        // This prevents "no Session" error when mapper tries to access replies later
        saved.getReplies().size(); // Force initialization of lazy collection
        
        return saved;
    }

    /**
     * Delete comment (soft delete)
     * Rules:
     * - Post owner can delete any comment in their post
     * - Comment owner can only delete their own comment
     * - TH1: When deleting a root comment (parentComment is null), delete entire sub-tree (all levels recursively)
     * - TH2: When deleting a child comment (parentComment != null), delete only that comment, do NOT delete any replies
     */
    @CacheEvict(value = {"postDetails"}, allEntries = true)
    @Transactional
    public void deleteComment(@NonNull UUID commentId, @NonNull UUID residentId) {
        log.info("Deleting comment: {} by user: {}", commentId, residentId);

        MarketplaceComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found: " + commentId));

        MarketplacePost post = comment.getPost();
        UUID postOwnerId = post.getResidentId();
        UUID commentOwnerId = comment.getResidentId();

        // Check authorization: post owner OR comment owner
        boolean isPostOwner = postOwnerId.equals(residentId);
        boolean isCommentOwner = commentOwnerId.equals(residentId);

        if (!isPostOwner && !isCommentOwner) {
            throw new RuntimeException("Not authorized to delete this comment");
        }

        // TH1: If deleting a root comment (parentComment is null), delete entire sub-tree recursively
        if (comment.getParentComment() == null) {
            // Delete all replies recursively (all levels)
            List<MarketplaceComment> directChildren = comment.getReplies();
            if (directChildren != null && !directChildren.isEmpty()) {
                log.info("TH1: Deleting entire sub-tree of root comment {} ({} direct children)", commentId, directChildren.size());
                int deletedCount = deleteRepliesRecursively(directChildren, post);
                log.info("TH1: Deleted {} comments in sub-tree recursively", deletedCount);
            }
        } else {
            // TH2: Deleting a child comment (parentComment != null)
            // Only delete this comment, do NOT delete any replies
            // Move replies to parent comment so they remain visible
            List<MarketplaceComment> childReplies = comment.getReplies();
            if (childReplies != null && !childReplies.isEmpty()) {
                MarketplaceComment parentComment = comment.getParentComment();
                log.info("TH2: Deleting child comment {} only, moving {} replies to parent comment {}", 
                        commentId, childReplies.size(), parentComment.getId());
                
                // Move replies to parent comment
                for (MarketplaceComment reply : childReplies) {
                    reply.setParentComment(parentComment);
                    commentRepository.save(reply);
                }
            }
            log.info("TH2: Deleted child comment {} only, kept all replies", commentId);
        }

        // Mark the comment itself as deleted
        comment.markAsDeleted();
        commentRepository.save(comment);

        // Decrement comment count
        post.decrementCommentCount();
        postRepository.save(post);
    }

    /**
     * Recursively delete all replies to a comment
     * @param replies List of replies to delete
     * @param post The post to decrement comment count
     * @return Number of comments deleted
     */
    private int deleteRepliesRecursively(List<MarketplaceComment> replies, MarketplacePost post) {
        int deletedCount = 0;
        for (MarketplaceComment reply : replies) {
            // Recursively delete replies of this reply
            List<MarketplaceComment> nestedReplies = reply.getReplies();
            if (nestedReplies != null && !nestedReplies.isEmpty()) {
                deletedCount += deleteRepliesRecursively(nestedReplies, post);
            }
            
            // Mark this reply as deleted
            reply.markAsDeleted();
            commentRepository.save(reply);
            post.decrementCommentCount();
            deletedCount++;
        }
        return deletedCount;
    }
}

