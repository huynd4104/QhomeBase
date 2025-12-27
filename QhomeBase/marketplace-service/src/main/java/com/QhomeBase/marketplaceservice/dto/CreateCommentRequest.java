package com.QhomeBase.marketplaceservice.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommentRequest {

    @Size(max = 2000, message = "Content must not exceed 2000 characters")
    private String content; // Optional - can be empty if imageUrl or videoUrl is provided

    private UUID parentCommentId; // For replies

    private String imageUrl; // URL of image attached to comment

    private String videoUrl; // URL of video attached to comment
}

