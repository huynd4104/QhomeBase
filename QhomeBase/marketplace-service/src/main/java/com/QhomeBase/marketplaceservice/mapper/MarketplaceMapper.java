package com.QhomeBase.marketplaceservice.mapper;

import com.QhomeBase.marketplaceservice.dto.*;
import com.QhomeBase.marketplaceservice.model.MarketplaceCategory;
import com.QhomeBase.marketplaceservice.model.MarketplaceComment;
import com.QhomeBase.marketplaceservice.model.MarketplacePost;
import com.QhomeBase.marketplaceservice.model.MarketplacePostImage;
import com.QhomeBase.marketplaceservice.service.ResidentInfoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MarketplaceMapper {

    private final ResidentInfoService residentInfoService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PostResponse toPostResponse(MarketplacePost post) {
        // Fetch resident info for author
        ResidentInfoResponse author = null;
        try {
            author = residentInfoService.getResidentInfo(post.getResidentId());
            if (author == null) {
                System.err.println("WARNING: Author is null for post residentId: " + post.getResidentId() + ", postId: " + post.getId());
            } else {
                System.out.println("✅ Author info for post " + post.getId() + ": name=" + author.getName() + ", unitNumber=" + author.getUnitNumber());
            }
        } catch (Exception e) {
            System.err.println("ERROR: Exception when fetching author for post " + post.getId() + ": " + e.getMessage());
            e.printStackTrace();
        }
        
        // Debug: Check images - safely access to avoid LazyInitializationException
        List<MarketplacePostImage> images = post.getImages();
        int imageCount = images != null ? images.size() : 0;
        System.out.println("🖼️ [MarketplaceMapper] Post " + post.getId() + " has " + imageCount + " images");
        if (images != null && !images.isEmpty()) {
            images.forEach(img -> {
                System.out.println("  - Image ID: " + img.getId() + ", URL: " + img.getImageUrl() + ", Thumbnail: " + img.getThumbnailUrl());
            });
        } else {
            System.out.println("⚠️ [MarketplaceMapper] Post " + post.getId() + " has no images");
        }
        
        // Debug: Log contactInfo from post entity
        String contactInfoJson = post.getContactInfo();
        System.out.println("📞 [MarketplaceMapper] Post " + post.getId() + " contactInfo from entity: " + contactInfoJson);
        
        return PostResponse.builder()
                .id(post.getId())
                .residentId(post.getResidentId())
                .buildingId(post.getBuildingId())
                .title(post.getTitle())
                .description(post.getDescription())
                .price(post.getPrice())
                .category(post.getCategory())
                .categoryName(post.getCategory()) // Will be enhanced with category lookup
                .status(post.getStatus().name())
                .scope(post.getScope() != null ? post.getScope().name() : "BUILDING")
                .contactInfo(toContactInfoResponse(contactInfoJson))
                .location(post.getLocation())
                .viewCount(post.getViewCount())
                .commentCount(post.getCommentCount())
                .images(images != null ? images.stream()
                        .map(this::toPostImageResponse)
                        .collect(Collectors.toList()) : new java.util.ArrayList<>())
                .videoUrl(normalizeVideoUrl(post.getVideoUrl())) // Normalize to relative path
                .author(author)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    public PostImageResponse toPostImageResponse(MarketplacePostImage image) {
        return PostImageResponse.builder()
                .id(image.getId())
                .imageUrl(image.getImageUrl())
                .thumbnailUrl(image.getThumbnailUrl())
                .sortOrder(image.getSortOrder())
                .build();
    }

    public ContactInfoResponse toContactInfoResponse(String contactInfoJson) {
        System.out.println("📞 [MarketplaceMapper] Parsing contactInfo JSON: " + contactInfoJson);
        
        if (contactInfoJson == null || contactInfoJson.isEmpty() || contactInfoJson.trim().equals("{}")) {
            System.out.println("⚠️ [MarketplaceMapper] ContactInfo is null or empty");
            return ContactInfoResponse.builder()
                    .showPhone(false)
                    .showEmail(false)
                    .build();
        }
        
        try {
            // Parse JSON string to ContactInfoRequest
            ContactInfoRequest contactInfo = objectMapper.readValue(contactInfoJson, ContactInfoRequest.class);
            System.out.println("✅ [MarketplaceMapper] Parsed ContactInfoRequest - phone: " + contactInfo.getPhone() + ", email: " + contactInfo.getEmail() + ", showPhone: " + contactInfo.getShowPhone() + ", showEmail: " + contactInfo.getShowEmail());
            
            // Build response with actual phone and email
            ContactInfoResponse.ContactInfoResponseBuilder builder = ContactInfoResponse.builder()
                    .phone(contactInfo.getPhone())
                    .email(contactInfo.getEmail())
                    .showPhone(contactInfo.getShowPhone() != null ? contactInfo.getShowPhone() : true)
                    .showEmail(contactInfo.getShowEmail() != null ? contactInfo.getShowEmail() : false);
            
            // Generate masked phone display if phone exists
            if (contactInfo.getPhone() != null && !contactInfo.getPhone().trim().isEmpty()) {
                String phone = contactInfo.getPhone().trim();
                if (phone.length() > 4) {
                    builder.phoneDisplay("***" + phone.substring(phone.length() - 4));
                } else {
                    builder.phoneDisplay("***");
                }
            }
            
            ContactInfoResponse response = builder.build();
            System.out.println("✅ [MarketplaceMapper] Built ContactInfoResponse - phone: " + response.getPhone() + ", email: " + response.getEmail() + ", showPhone: " + response.getShowPhone() + ", showEmail: " + response.getShowEmail());
            return response;
        } catch (Exception e) {
            System.err.println("❌ [MarketplaceMapper] ERROR: Failed to parse contactInfo JSON: " + contactInfoJson);
            System.err.println("❌ [MarketplaceMapper] Exception: " + e.getMessage());
            e.printStackTrace();
            // Return empty response on parse error
            return ContactInfoResponse.builder()
                    .showPhone(false)
                    .showEmail(false)
                    .build();
        }
    }

    public CommentResponse toCommentResponse(MarketplaceComment comment) {
        // Fetch resident info for author
        ResidentInfoResponse author = null;
        try {
            author = residentInfoService.getResidentInfo(comment.getResidentId());
            if (author == null) {
                System.err.println("WARNING: Author is null for comment residentId: " + comment.getResidentId() + ", commentId: " + comment.getId());
            } else {
                System.out.println("✅ Author info for comment " + comment.getId() + ": name=" + author.getName() + ", unitNumber=" + author.getUnitNumber());
            }
        } catch (Exception e) {
            System.err.println("ERROR: Exception when fetching author for comment " + comment.getId() + ": " + e.getMessage());
            e.printStackTrace();
        }
        
        return CommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .residentId(comment.getResidentId())
                .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null)
                .content(comment.isDeleted() ? "[Đã xóa]" : comment.getContent())
                .author(author)
                .replies(comment.getReplies().stream()
                        .map(this::toCommentResponse)
                        .collect(Collectors.toList()))
                .replyCount(comment.getReplies().size())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .isDeleted(comment.isDeleted())
                .imageUrl(comment.getImageUrl())
                .videoUrl(normalizeVideoUrl(comment.getVideoUrl())) // Normalize to relative path
                .build();
    }

    /**
     * Normalize video URL to relative path
     * If URL is full URL (contains http:// or https://), extract relative path
     * Otherwise return as-is (already relative or null)
     */
    private String normalizeVideoUrl(String videoUrl) {
        if (videoUrl == null || videoUrl.isEmpty()) {
            return null;
        }
        
        // If URL contains /api/videos/stream/, extract relative path
        if (videoUrl.contains("/api/videos/stream/")) {
            String[] parts = videoUrl.split("/api/videos/stream/");
            if (parts.length > 1) {
                String videoIdStr = parts[1].split("\\?")[0]; // Remove query params if any
                return "/api/videos/stream/" + videoIdStr;
            }
        }
        
        // If already relative path (starts with /), return as-is
        if (videoUrl.startsWith("/")) {
            return videoUrl;
        }
        
        // If it's a full URL (starts with http:// or https://), try to extract path
        if (videoUrl.startsWith("http://") || videoUrl.startsWith("https://")) {
            try {
                java.net.URL url = new java.net.URL(videoUrl);
                String path = url.getPath();
                if (path != null && !path.isEmpty()) {
                    return path;
                }
            } catch (Exception e) {
                // If URL parsing fails, return null
                return null;
            }
        }
        
        // Fallback: return as-is (might be invalid, but let client handle it)
        return videoUrl;
    }

    public CategoryResponse toCategoryResponse(MarketplaceCategory category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .code(category.getCode())
                .name(category.getName())
                .nameEn(category.getNameEn())
                .icon(category.getIcon())
                .displayOrder(category.getDisplayOrder())
                .active(category.getActive())
                .build();
    }

    public PostPagedResponse toPostPagedResponse(Page<MarketplacePost> page) {
        List<PostResponse> content = page.getContent().stream()
                .map(this::toPostResponse)
                .collect(Collectors.toList());

        return PostPagedResponse.builder()
                .content(content)
                .currentPage(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .build();
    }
}

