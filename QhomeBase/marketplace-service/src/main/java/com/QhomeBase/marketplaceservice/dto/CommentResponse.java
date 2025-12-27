package com.QhomeBase.marketplaceservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    private UUID id;
    private UUID postId;
    private UUID residentId;
    private UUID parentCommentId;
    private String content;
    private ResidentInfoResponse author;
    private List<CommentResponse> replies;
    private Integer replyCount;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Boolean isDeleted;

    private String imageUrl; // URL of image attached to comment

    private String videoUrl; // URL of video attached to comment
}

