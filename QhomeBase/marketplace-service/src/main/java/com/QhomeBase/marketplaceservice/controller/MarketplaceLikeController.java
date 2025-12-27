package com.QhomeBase.marketplaceservice.controller;

import com.QhomeBase.marketplaceservice.security.UserPrincipal;
import com.QhomeBase.marketplaceservice.service.MarketplaceLikeService;
import com.QhomeBase.marketplaceservice.service.MarketplaceNotificationService;
import com.QhomeBase.marketplaceservice.service.MarketplacePostService;
import com.QhomeBase.marketplaceservice.service.RateLimitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/posts/{postId}/like")
@RequiredArgsConstructor
@Tag(name = "Marketplace Likes", description = "APIs for managing post likes")
public class MarketplaceLikeController {

    private final MarketplaceLikeService likeService;
    private final RateLimitService rateLimitService;
    private final MarketplaceNotificationService notificationService;
    private final MarketplacePostService postService;

    @PostMapping
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Toggle like", description = "Like or unlike a post")
    public ResponseEntity<Map<String, Object>> toggleLike(
            @PathVariable UUID postId,
            Authentication authentication) {
        
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();

        // Rate limiting
        if (!rateLimitService.canLike(userId)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .build();
        }

        boolean isLiked = likeService.toggleLike(postId, userId);
        Long likeCount = likeService.getLikeCount(postId);
        
        // Get post to get all stats
        var post = postService.getPostById(postId);
        
        // Send realtime stats update
        notificationService.notifyPostStatsUpdate(
                postId, 
                post.getLikeCount(), 
                post.getCommentCount(), 
                post.getViewCount()
        );
        
        // Send like notification
        notificationService.notifyNewLike(postId, userId, "User", isLiked);

        Map<String, Object> response = new HashMap<>();
        response.put("isLiked", isLiked);
        response.put("likeCount", likeCount);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Check if liked", description = "Check if the current user liked the post")
    public ResponseEntity<Map<String, Object>> checkLiked(
            @PathVariable UUID postId,
            Authentication authentication) {
        
        boolean isLiked = false;
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UUID userId = ((UserPrincipal) authentication.getPrincipal()).uid();
            isLiked = likeService.isLikedByUser(postId, userId);
        }

        Long likeCount = likeService.getLikeCount(postId);

        Map<String, Object> response = new HashMap<>();
        response.put("isLiked", isLiked);
        response.put("likeCount", likeCount);

        return ResponseEntity.ok(response);
    }
}

