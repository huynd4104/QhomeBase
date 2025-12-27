package com.QhomeBase.marketplaceservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for real-time notifications via WebSocket and FCM push notifications
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MarketplaceNotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationClient notificationClient;

    /**
     * Notify new comment on a post
     * Sends both WebSocket realtime notification and FCM push notification to post owner
     */
    public void notifyNewComment(UUID postId, UUID commentId, UUID authorId, String authorName, UUID postOwnerResidentId, UUID parentCommentId) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "NEW_COMMENT");
        message.put("postId", postId.toString());
        message.put("commentId", commentId.toString());
        message.put("authorId", authorId.toString());
        message.put("authorName", authorName);
        message.put("timestamp", System.currentTimeMillis());
        
        // Add parentCommentId if this is a reply (to help Flutter navigate to correct position)
        if (parentCommentId != null) {
            message.put("parentCommentId", parentCommentId.toString());
            message.put("isReply", true);
        } else {
            message.put("isReply", false);
        }
        
        // Add navigation action to help Flutter navigate to the comment
        message.put("action", "navigate_to_comment");

        // Send WebSocket realtime notification to post owner and all users viewing the post
        messagingTemplate.convertAndSend("/topic/marketplace/post/" + postId + "/comments", message);
        log.info("✅ [MarketplaceNotificationService] Sent WebSocket new comment notification for post: {} (commentId: {}, parentCommentId: {})", 
                postId, commentId, parentCommentId);
        
        // Send FCM push notification to post owner (chủ post)
        if (postOwnerResidentId != null && !postOwnerResidentId.equals(authorId)) {
            // Only send push notification if comment author is not the post owner (avoid self-notification)
            try {
                String title = "Có người bình luận vào bài đăng của bạn";
                String body = String.format("%s đã bình luận vào bài đăng của bạn", authorName != null ? authorName : "Ai đó");
                
                Map<String, String> dataPayload = new HashMap<>();
                dataPayload.put("type", "MARKETPLACE_COMMENT");
                dataPayload.put("postId", postId.toString());
                dataPayload.put("commentId", commentId.toString());
                dataPayload.put("authorId", authorId.toString());
                dataPayload.put("authorName", authorName != null ? authorName : "User");
                // Add parentCommentId for navigation
                if (parentCommentId != null) {
                    dataPayload.put("parentCommentId", parentCommentId.toString());
                }
                dataPayload.put("action", "navigate_to_comment");
                
                notificationClient.sendPushNotificationToResident(postOwnerResidentId, title, body, dataPayload);
                log.info("✅ [MarketplaceNotificationService] Sent FCM push notification to post owner (residentId: {}) for comment on post: {}", 
                        postOwnerResidentId, postId);
            } catch (Exception e) {
                log.error("❌ [MarketplaceNotificationService] Error sending FCM push notification to post owner: {}", e.getMessage(), e);
                // Don't throw - continue even if push notification fails
            }
        } else {
            log.debug("ℹ️ [MarketplaceNotificationService] Skipping FCM push notification - post owner is the comment author");
        }
    }

    /**
     * Notify new like on a post
     */
    public void notifyNewLike(UUID postId, UUID userId, String userName, boolean isLiked) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", isLiked ? "POST_LIKED" : "POST_UNLIKED");
        message.put("postId", postId.toString());
        message.put("userId", userId.toString());
        message.put("userName", userName);
        message.put("timestamp", System.currentTimeMillis());

        messagingTemplate.convertAndSend("/topic/marketplace/post/" + postId + "/likes", message);
        log.info("Sent like notification for post: {} (liked: {})", postId, isLiked);
    }

    /**
     * Notify new post in building
     */
    public void notifyNewPost(UUID buildingId, UUID postId, String title) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "NEW_POST");
        message.put("postId", postId.toString());
        message.put("title", title);
        message.put("timestamp", System.currentTimeMillis());

        messagingTemplate.convertAndSend("/topic/marketplace/building/" + buildingId + "/posts", message);
        log.info("Sent new post notification for building: {}", buildingId);
    }

    /**
     * Notify post status change
     */
    public void notifyPostStatusChange(UUID postId, String status) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "POST_STATUS_CHANGED");
        message.put("postId", postId.toString());
        message.put("status", status);
        message.put("timestamp", System.currentTimeMillis());

        messagingTemplate.convertAndSend("/topic/marketplace/post/" + postId + "/status", message);
        log.info("Sent post status change notification: {} -> {}", postId, status);
    }

    /**
     * Notify post stats update (like count, comment count, view count)
     */
    public void notifyPostStatsUpdate(UUID postId, Long likeCount, Long commentCount, Long viewCount) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "POST_STATS_UPDATE");
        message.put("postId", postId.toString());
        message.put("likeCount", likeCount);
        message.put("commentCount", commentCount);
        message.put("viewCount", viewCount);
        message.put("timestamp", System.currentTimeMillis());

        messagingTemplate.convertAndSend("/topic/marketplace/post/" + postId + "/stats", message);
        log.debug("Sent post stats update for post: {} (likes: {}, comments: {}, views: {})", 
                postId, likeCount, commentCount, viewCount);
    }
}

