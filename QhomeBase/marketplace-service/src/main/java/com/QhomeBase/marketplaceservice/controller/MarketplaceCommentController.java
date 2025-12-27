package com.QhomeBase.marketplaceservice.controller;

import com.QhomeBase.marketplaceservice.dto.CommentPagedResponse;
import com.QhomeBase.marketplaceservice.dto.CommentResponse;
import com.QhomeBase.marketplaceservice.dto.CreateCommentRequest;
import com.QhomeBase.marketplaceservice.mapper.MarketplaceMapper;
import com.QhomeBase.marketplaceservice.model.MarketplaceComment;
import com.QhomeBase.marketplaceservice.security.UserPrincipal;
import com.QhomeBase.marketplaceservice.service.MarketplaceCommentService;
import com.QhomeBase.marketplaceservice.service.MarketplaceNotificationService;
import com.QhomeBase.marketplaceservice.service.MarketplacePostService;
import com.QhomeBase.marketplaceservice.service.RateLimitService;
import com.QhomeBase.marketplaceservice.service.ResidentInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/posts/{postId}/comments")
@RequiredArgsConstructor
@Tag(name = "Marketplace Comments", description = "APIs for managing post comments")
public class MarketplaceCommentController {

    private final MarketplaceCommentService commentService;
    private final RateLimitService rateLimitService;
    private final MarketplaceMapper mapper;
    private final MarketplaceNotificationService notificationService;
    private final MarketplacePostService postService;
    private final ResidentInfoService residentInfoService;

    @GetMapping
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Get comments", description = "Get paginated comments for a post")
    public ResponseEntity<CommentPagedResponse> getComments(
            @PathVariable UUID postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        
        // Get current user info for filtering blocked comments
        UUID currentResidentId = null;
        String accessToken = null;
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            UUID userId = principal.uid();
            accessToken = principal.token();
            currentResidentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        }
        
        Page<MarketplaceComment> commentsPage = commentService.getComments(postId, currentResidentId, accessToken, page, size);
        List<CommentResponse> content = commentsPage.getContent().stream()
                .map(mapper::toCommentResponse)
                .collect(Collectors.toList());
        
        CommentPagedResponse response = CommentPagedResponse.builder()
                .content(content)
                .currentPage(commentsPage.getNumber())
                .pageSize(commentsPage.getSize())
                .totalElements(commentsPage.getTotalElements())
                .totalPages(commentsPage.getTotalPages())
                .hasNext(commentsPage.hasNext())
                .hasPrevious(commentsPage.hasPrevious())
                .build();
        
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Add comment", description = "Add a new comment to a post")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable UUID postId,
            @Valid @RequestBody CreateCommentRequest request,
            Authentication authentication) {
        
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        String accessToken = principal.token();

        // Get residentId from userId
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            log.error("Cannot find residentId for userId: {}", userId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        // Rate limiting
        if (!rateLimitService.canComment(userId)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        MarketplaceComment comment = commentService.addComment(
                postId, residentId, request.getContent(), request.getParentCommentId(),
                request.getImageUrl(), request.getVideoUrl(), residentId, accessToken
        );
        
        // Get post to get all stats and post owner info
        var post = postService.getPostById(postId);
        
        // Send realtime stats update
        notificationService.notifyPostStatsUpdate(
                postId, 
                post.getLikeCount(), 
                post.getCommentCount(), 
                post.getViewCount()
        );
        
        // Note: New comment notification (WebSocket + FCM push) is already sent by MarketplaceCommentService.addComment
        // with the actual author name fetched from ResidentInfoService

        CommentResponse response = mapper.toCommentResponse(comment);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{commentId}")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Update comment", description = "Update an existing comment")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable UUID postId,
            @PathVariable UUID commentId,
            @Valid @RequestBody CreateCommentRequest request,
            Authentication authentication) {
        
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        String accessToken = principal.token();

        // Get residentId from userId
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            log.error("Cannot find residentId for userId: {}", userId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        MarketplaceComment comment = commentService.updateComment(
                commentId, residentId, request.getContent(), 
                request.getImageUrl(), request.getVideoUrl());
        CommentResponse response = mapper.toCommentResponse(comment);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{commentId}")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Delete comment", description = "Delete a comment (soft delete)")
    public ResponseEntity<Void> deleteComment(
            @PathVariable UUID postId,
            @PathVariable UUID commentId,
            Authentication authentication) {
        
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        String accessToken = principal.token();

        // Get residentId from userId
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            log.error("Cannot find residentId for userId: {}", userId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        commentService.deleteComment(commentId, residentId);
        
        // Get post to get updated stats
        var post = postService.getPostById(postId);
        
        // Send realtime stats update
        notificationService.notifyPostStatsUpdate(
                postId, 
                post.getLikeCount(), 
                post.getCommentCount(), 
                post.getViewCount()
        );
        
        return ResponseEntity.noContent().build();
    }
}

