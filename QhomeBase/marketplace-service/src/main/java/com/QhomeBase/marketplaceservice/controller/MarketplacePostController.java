package com.QhomeBase.marketplaceservice.controller;

import com.QhomeBase.marketplaceservice.dto.*;
import com.QhomeBase.marketplaceservice.mapper.MarketplaceMapper;
import com.QhomeBase.marketplaceservice.model.MarketplacePost;
import com.QhomeBase.marketplaceservice.model.MarketplacePostImage;
import com.QhomeBase.marketplaceservice.model.PostStatus;
import com.QhomeBase.marketplaceservice.model.PostScope;
import com.QhomeBase.marketplaceservice.repository.MarketplacePostImageRepository;
import com.QhomeBase.marketplaceservice.security.UserPrincipal;
import com.QhomeBase.marketplaceservice.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
@Tag(name = "Marketplace Posts", description = "APIs for managing marketplace posts")
public class MarketplacePostController {
    
    private final ObjectMapper objectMapper;

    private final MarketplacePostService postService;
    private final MarketplaceNotificationService notificationService;
    private final MarketplaceCategoryService categoryService;
    private final RateLimitService rateLimitService;
    private final ImageProcessingService imageProcessingService;
    private final VideoProcessingService videoProcessingService;
    private final FileStorageService fileStorageService;
    private final AsyncImageProcessingService asyncImageProcessingService;
    private final MarketplaceMapper mapper;
    private final ResidentInfoService residentInfoService;
    private final MarketplacePostImageRepository imageRepository;

    @GetMapping
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Get posts with filters", description = "Get paginated list of posts with optional filters")
    public ResponseEntity<PostPagedResponse> getPosts(
            @RequestParam(required = false) UUID buildingId,
            @RequestParam(defaultValue = "ACTIVE") String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String filterScope,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        PostStatus postStatus = PostStatus.valueOf(status.toUpperCase());
        
        log.info("🔍 [MarketplacePostController] getPosts request: buildingId={}, status={}, filterScope={}, page={}, size={}", 
                buildingId, status, filterScope, page, size);
        
        // Get current user info for filtering blocked posts
        UUID currentResidentId = null;
        String accessToken = null;
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            UUID userId = principal.uid();
            accessToken = principal.token();
            currentResidentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        }
        
        Page<MarketplacePost> posts = postService.getPosts(
                buildingId, postStatus, category, minPrice, maxPrice, search, sortBy, filterScope, 
                currentResidentId, accessToken, page, size
        );
        
        log.info("🔍 [MarketplacePostController] getPosts response: {} posts (total: {})", 
                posts.getContent().size(), posts.getTotalElements());

        PostPagedResponse response = mapper.toPostPagedResponse(posts);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Get post by ID", description = "Get detailed information about a specific post")
    public ResponseEntity<PostResponse> getPostById(
            @PathVariable UUID id,
            Authentication authentication) {
        
        // Get current user info for filtering blocked posts
        UUID currentResidentId = null;
        String accessToken = null;
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            UUID userId = principal.uid();
            accessToken = principal.token();
            currentResidentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        }
        
        MarketplacePost post = postService.getPostById(id, currentResidentId, accessToken);
        
        // Increment view count
        postService.incrementViewCount(id);
        
        // Reload post to get updated view count
        post = postService.getPostById(id, currentResidentId, accessToken);
        
        // Send realtime stats update
        notificationService.notifyPostStatsUpdate(
                id, 
                0L, // likeCount removed
                post.getCommentCount(), 
                post.getViewCount()
        );

        PostResponse response = mapper.toPostResponse(post);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Create new post", description = "Create a new marketplace post")
    public ResponseEntity<PostResponse> createPost(
            @RequestPart("data") String dataJson,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @RequestPart(value = "video", required = false) MultipartFile video,
            Authentication authentication) {
        
        // Manually deserialize JSON string to CreatePostRequest
        CreatePostRequest request;
        try {
            log.info("📝 [MarketplacePostController] Raw data JSON: {}", dataJson);
            request = objectMapper.readValue(dataJson, CreatePostRequest.class);
            log.info("✅ [MarketplacePostController] Deserialized CreatePostRequest successfully");
        } catch (Exception e) {
            log.error("❌ [MarketplacePostController] Error deserializing request: {}", e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
        
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        String accessToken = principal.token();

        // Debug: Log request data
        log.info("📝 [MarketplacePostController] Received createPost request:");
        log.info("   - buildingId: {}", request.getBuildingId());
        log.info("   - title: {}", request.getTitle());
        log.info("   - category: {}", request.getCategory());
        log.info("   - contactInfo: {}", request.getContactInfo());
        if (request.getContactInfo() != null) {
            log.info("   - contactInfo.phone: {}", request.getContactInfo().getPhone());
            log.info("   - contactInfo.email: {}", request.getContactInfo().getEmail());
            log.info("   - contactInfo.showPhone: {}", request.getContactInfo().getShowPhone());
            log.info("   - contactInfo.showEmail: {}", request.getContactInfo().getShowEmail());
        }

        // Get residentId from userId
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            log.error("Cannot find residentId for userId: {}", userId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .build();
        }

        // Rate limiting
        if (!rateLimitService.canCreatePost(userId)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .build();
        }

        // Validate images
        if (images != null && !images.isEmpty()) {
            if (images.size() > 10) {
                return ResponseEntity.badRequest().build();
            }
            for (MultipartFile image : images) {
                try {
                    imageProcessingService.validateImage(image);
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().build();
                }
            }
        }

        // Validate video (max 20 seconds, should be validated on client side)
        if (video != null && !video.isEmpty()) {
            try {
                videoProcessingService.validateVideo(video);
            } catch (IllegalArgumentException e) {
                log.error("Invalid video: {}", e.getMessage());
                return ResponseEntity.badRequest().build();
            }
        }

        // Parse scope from request
        PostScope postScope = PostScope.BUILDING; // Default
        if (request.getScope() != null && !request.getScope().isEmpty()) {
            try {
                postScope = PostScope.valueOf(request.getScope().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid scope value: {}, using default BUILDING", request.getScope());
            }
        }
        
        // Create post
        MarketplacePost post = MarketplacePost.builder()
                .residentId(residentId)
                .buildingId(request.getBuildingId())
                .title(request.getTitle())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(request.getCategory())
                .status(PostStatus.ACTIVE)
                .location(request.getLocation())
                .scope(postScope)
                .videoUrl(request.getVideoUrl()) // Video URL from data-docs-service
                .build();

        // Handle contact info - serialize to JSON string
        if (request.getContactInfo() != null) {
            try {
                String contactInfoJson = objectMapper.writeValueAsString(request.getContactInfo());
                log.info("📞 [MarketplacePostController] Serializing contactInfo: phone={}, email={}, showPhone={}, showEmail={}", 
                    request.getContactInfo().getPhone(), 
                    request.getContactInfo().getEmail(),
                    request.getContactInfo().getShowPhone(),
                    request.getContactInfo().getShowEmail());
                log.info("📞 [MarketplacePostController] Serialized JSON: {}", contactInfoJson);
                post.setContactInfo(contactInfoJson);
                log.info("✅ [MarketplacePostController] Set contactInfo to post: {}", contactInfoJson);
            } catch (Exception e) {
                log.error("❌ [MarketplacePostController] Error serializing contact info: {}", e.getMessage());
                e.printStackTrace();
                post.setContactInfo("{}");
            }
        } else {
            log.warn("⚠️ [MarketplacePostController] ContactInfo is null in request - post will have no contact info");
        }
        
        log.info("📝 [MarketplacePostController] Post before save - contactInfo: {}", post.getContactInfo());

        MarketplacePost saved = postService.createPost(post);

        // Upload images synchronously first to get URLs immediately
        // Then process thumbnails asynchronously
        if (images != null && !images.isEmpty()) {
            for (int i = 0; i < images.size(); i++) {
                MultipartFile image = images.get(i);
                try {
                    // Upload original image synchronously to get URL immediately
                    Map<String, String> imageUrls = fileStorageService.uploadImage(image, saved.getId().toString());
                    String originalUrl = imageUrls.get("original");
                    
                    if (originalUrl != null && !originalUrl.isEmpty()) {
                        // Save image to database immediately with original URL
                        MarketplacePostImage postImage = MarketplacePostImage.builder()
                                .post(saved)
                                .imageUrl(originalUrl)
                                .thumbnailUrl(null) // Will be set later by async processing
                                .sortOrder(i)
                                .build();
                        imageRepository.save(postImage);
                        log.info("✅ [MarketplacePostController] Saved image immediately: {}", originalUrl);
                        
                        // Process thumbnail asynchronously and update later
                        byte[] imageBytes = image.getBytes();
                        String baseFileName = UUID.randomUUID().toString() + ".jpg";
                        asyncImageProcessingService.processThumbnailAsync(
                                imageBytes,
                                image.getOriginalFilename(),
                                image.getContentType(),
                                saved.getId(),
                                baseFileName,
                                postImage.getId()); // Pass image ID to update later
                    }
                } catch (Exception e) {
                    log.error("Error uploading image: {}", e.getMessage(), e);
                }
            }
        }

        // Handle video: use videoUrl from request if provided, otherwise upload video file
        // Video is now stored in post.videoUrl field, NOT in images table
        if (request.getVideoUrl() != null && !request.getVideoUrl().trim().isEmpty()) {
            saved.setVideoUrl(request.getVideoUrl().trim());
            saved = postService.updatePost(saved.getId(), saved);
            log.info("✅ [MarketplacePostController] Set videoUrl from request: {}", request.getVideoUrl());
        } else if (video != null && !video.isEmpty()) {
            try {
                // Upload video to ImageKit (fallback if video file is provided directly)
                Map<String, String> videoUrls = fileStorageService.uploadVideo(video, saved.getId().toString());
                String videoUrl = videoUrls.get("original");
                saved.setVideoUrl(videoUrl);
                saved = postService.updatePost(saved.getId(), saved);
                log.info("✅ [MarketplacePostController] Uploaded video to ImageKit and set videoUrl: {}", videoUrl);
            } catch (Exception e) {
                log.error("Error uploading video: {}", e.getMessage(), e);
            }
        }

        // Refresh post to ensure images are loaded (lazy loading)
        MarketplacePost refreshedPost = postService.getPostById(saved.getId());

        // Notify new post
        notificationService.notifyNewPost(request.getBuildingId(), refreshedPost.getId(), refreshedPost.getTitle());
        
        // Send initial stats
        notificationService.notifyPostStatsUpdate(
                refreshedPost.getId(), 
                0L, // likeCount removed
                refreshedPost.getCommentCount(), 
                refreshedPost.getViewCount()
        );

        PostResponse response = mapper.toPostResponse(refreshedPost);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Update post", description = "Update an existing post")
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable UUID id,
            @RequestPart("data") String dataJson,
            @RequestPart(value = "images", required = false) List<MultipartFile> newImages,
            @RequestPart(value = "video", required = false) MultipartFile video,
            Authentication authentication) {
        
        // Manually deserialize JSON string to UpdatePostRequest
        UpdatePostRequest request;
        try {
            log.info("📝 [MarketplacePostController] Raw update data JSON: {}", dataJson);
            request = objectMapper.readValue(dataJson, UpdatePostRequest.class);
            log.info("✅ [MarketplacePostController] Deserialized UpdatePostRequest successfully");
        } catch (Exception e) {
            log.error("❌ [MarketplacePostController] Error deserializing update request: {}", e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
        
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        String accessToken = principal.token();

        // Get residentId from userId
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            log.error("Resident not found for userId: {}", userId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        MarketplacePost post = postService.getPostById(id);
        if (!post.getResidentId().equals(residentId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Update fields
        if (request.getTitle() != null) post.setTitle(request.getTitle());
        if (request.getDescription() != null) post.setDescription(request.getDescription());
        if (request.getPrice() != null) post.setPrice(request.getPrice());
        if (request.getCategory() != null) post.setCategory(request.getCategory());
        if (request.getLocation() != null) post.setLocation(request.getLocation());
        
        // Handle contact info - serialize to JSON string
        if (request.getContactInfo() != null) {
            try {
                String contactInfoJson = objectMapper.writeValueAsString(request.getContactInfo());
                log.info("📞 [MarketplacePostController] Updating contactInfo: phone={}, email={}, showPhone={}, showEmail={}", 
                    request.getContactInfo().getPhone(), 
                    request.getContactInfo().getEmail(),
                    request.getContactInfo().getShowPhone(),
                    request.getContactInfo().getShowEmail());
                log.info("📞 [MarketplacePostController] Serialized JSON: {}", contactInfoJson);
                post.setContactInfo(contactInfoJson);
            } catch (Exception e) {
                log.error("❌ [MarketplacePostController] Error serializing contact info: {}", e.getMessage());
                e.printStackTrace();
            }
        }

        MarketplacePost updated = postService.updatePost(id, post);

        // Validate video if provided
        if (video != null && !video.isEmpty()) {
            try {
                videoProcessingService.validateVideo(video);
            } catch (IllegalArgumentException e) {
                log.error("Invalid video: {}", e.getMessage());
                return ResponseEntity.badRequest().build();
            }
        }

        // Handle video deletion
        if (request.getVideoToDelete() != null && !request.getVideoToDelete().isEmpty()) {
            try {
                UUID videoId = UUID.fromString(request.getVideoToDelete());
                // Find video in database (stored as MarketplacePostImage)
                var videoOpt = imageRepository.findById(videoId);
                if (videoOpt.isPresent()) {
                    MarketplacePostImage videoImage = videoOpt.get();
                    // Verify video belongs to this post
                    if (videoImage.getPost().getId().equals(id)) {
                        // Delete from database
                        imageRepository.delete(videoImage);
                        log.info("Deleted video from database: {}", videoId);
                        
                        // Delete from storage
                        String videoUrl = videoImage.getImageUrl();
                        if (videoUrl != null && videoUrl.contains("/uploads/")) {
                            String[] parts = videoUrl.split("/uploads/");
                            if (parts.length > 1) {
                                String filePath = parts[1]; // {postId}/{fileName}
                                String[] pathParts = filePath.split("/");
                                if (pathParts.length > 1) {
                                    String fileName = pathParts[1];
                                    fileStorageService.deleteImage(id.toString(), fileName);
                                    log.info("Deleted video file from storage: {}", fileName);
                                }
                            }
                        }
                    } else {
                        log.warn("Video {} does not belong to post {}", videoId, id);
                    }
                } else {
                    log.warn("Video not found in database: {}", videoId);
                }
            } catch (Exception e) {
                log.error("Error deleting video {}: {}", request.getVideoToDelete(), e.getMessage(), e);
            }
        }

        // Handle image deletion
        if (request.getImagesToDelete() != null && !request.getImagesToDelete().isEmpty()) {
            for (String imageIdStr : request.getImagesToDelete()) {
                try {
                    UUID imageId = UUID.fromString(imageIdStr);
                    // Find image in database
                    var imageOpt = imageRepository.findById(imageId);
                    if (imageOpt.isPresent()) {
                        MarketplacePostImage image = imageOpt.get();
                        // Verify image belongs to this post
                        if (image.getPost().getId().equals(id)) {
                            // Delete from database
                            imageRepository.delete(image);
                            log.info("Deleted image from database: {}", imageId);
                            
                            // Delete from storage
                            // Extract filename from imageUrl (format: /api/marketplace/uploads/{postId}/{fileName})
                            String imageUrl = image.getImageUrl();
                            if (imageUrl != null && imageUrl.contains("/uploads/")) {
                                String[] parts = imageUrl.split("/uploads/");
                                if (parts.length > 1) {
                                    String filePath = parts[1]; // {postId}/{fileName}
                                    String[] pathParts = filePath.split("/");
                                    if (pathParts.length > 1) {
                                        String fileName = pathParts[1];
                                        fileStorageService.deleteImage(id.toString(), fileName);
                                        log.info("Deleted image file from storage: {}", fileName);
                                    }
                                }
                            }
                            
                            // Also delete thumbnail if exists
                            String thumbnailUrl = image.getThumbnailUrl();
                            if (thumbnailUrl != null && thumbnailUrl.contains("/uploads/")) {
                                String[] parts = thumbnailUrl.split("/uploads/");
                                if (parts.length > 1) {
                                    String filePath = parts[1];
                                    String[] pathParts = filePath.split("/");
                                    if (pathParts.length > 1) {
                                        String fileName = pathParts[1];
                                        fileStorageService.deleteImage(id.toString(), fileName);
                                        log.info("Deleted thumbnail file from storage: {}", fileName);
                                    }
                                }
                            }
                        } else {
                            log.warn("Image {} does not belong to post {}", imageId, id);
                        }
                    } else {
                        log.warn("Image not found in database: {}", imageId);
                    }
                } catch (Exception e) {
                    log.error("Error deleting image {}: {}", imageIdStr, e.getMessage(), e);
                }
            }
        }

        // Handle new images - upload synchronously first to get URLs immediately
        // Then process thumbnails asynchronously
        if (newImages != null && !newImages.isEmpty()) {
            // Get current image count to determine sort order
            int currentImageCount = updated.getImages().size();
            for (int i = 0; i < newImages.size(); i++) {
                MultipartFile image = newImages.get(i);
                try {
                    // Upload original image synchronously to get URL immediately
                    Map<String, String> imageUrls = fileStorageService.uploadImage(image, updated.getId().toString());
                    String originalUrl = imageUrls.get("original");
                    
                    if (originalUrl != null && !originalUrl.isEmpty()) {
                        // Save image to database immediately with original URL
                        MarketplacePostImage postImage = MarketplacePostImage.builder()
                                .post(updated)
                                .imageUrl(originalUrl)
                                .thumbnailUrl(null) // Will be set later by async processing
                                .sortOrder(currentImageCount + i)
                                .build();
                        imageRepository.save(postImage);
                        log.info("✅ [MarketplacePostController] Saved new image immediately: {}", originalUrl);
                        
                        // Process thumbnail asynchronously and update later
                        byte[] imageBytes = image.getBytes();
                        String baseFileName = UUID.randomUUID().toString() + ".jpg";
                        asyncImageProcessingService.processThumbnailAsync(
                                imageBytes,
                                image.getOriginalFilename(),
                                image.getContentType(),
                                updated.getId(),
                                baseFileName,
                                postImage.getId()); // Pass image ID to update later
                    }
                } catch (Exception e) {
                    log.error("Error uploading image: {}", e.getMessage(), e);
                }
            }
        }

        // Handle video: use videoUrl from request if provided, otherwise upload video file
        // Video is now stored in post.videoUrl field, NOT in images table
        if (request.getVideoUrl() != null && !request.getVideoUrl().trim().isEmpty()) {
            updated.setVideoUrl(request.getVideoUrl().trim());
            log.info("✅ [MarketplacePostController] Set videoUrl from request: {}", request.getVideoUrl());
        } else if (video != null && !video.isEmpty()) {
            try {
                // Upload video to ImageKit (fallback if video file is provided directly)
                Map<String, String> videoUrls = fileStorageService.uploadVideo(video, updated.getId().toString());
                String videoUrl = videoUrls.get("original");
                updated.setVideoUrl(videoUrl);
                log.info("✅ [MarketplacePostController] Uploaded video to ImageKit and set videoUrl: {}", videoUrl);
            } catch (Exception e) {
                log.error("Error uploading video: {}", e.getMessage(), e);
            }
        }
        
        // Save post with updated videoUrl
        updated = postService.updatePost(id, updated);

        // Refresh post to ensure images are loaded (lazy loading)
        MarketplacePost refreshedPost = postService.getPostById(updated.getId());
        
        PostResponse response = mapper.toPostResponse(refreshedPost);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Delete post", description = "Delete a post")
    public ResponseEntity<Void> deletePost(
            @PathVariable UUID id,
            Authentication authentication) {
        
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        String accessToken = principal.token();

        // Get residentId from userId
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            log.error("Resident not found for userId: {}", userId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        MarketplacePost post = postService.getPostById(id);
        if (!post.getResidentId().equals(residentId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        postService.deletePost(id);
        fileStorageService.deletePostImages(id.toString());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Update post status", description = "Update post status (e.g., ACTIVE -> SOLD)")
    public ResponseEntity<PostResponse> updatePostStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequest request,
            Authentication authentication) {
        
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        String accessToken = principal.token();

        // Get residentId from userId
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            log.error("Resident not found for userId: {}", userId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        MarketplacePost post = postService.getPostById(id);
        if (!post.getResidentId().equals(residentId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        PostStatus status = PostStatus.valueOf(request.getStatus().toUpperCase());
        MarketplacePost updated = postService.updatePostStatus(id, status);

        // Notify status change
        // notificationService.notifyPostStatusChange(id, status.name());

        PostResponse response = mapper.toPostResponse(updated);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Get my posts", description = "Get posts created by the authenticated user")
    public ResponseEntity<PostPagedResponse> getMyPosts(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();
        String accessToken = principal.token();

        // Get residentId from userId
        UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
        if (residentId == null) {
            log.error("Resident not found for userId: {}", userId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        PostStatus postStatus = status != null ? PostStatus.valueOf(status.toUpperCase()) : null;
        Page<MarketplacePost> posts = postService.getMyPosts(residentId, postStatus, page, size);

        PostPagedResponse response = mapper.toPostPagedResponse(posts);
        return ResponseEntity.ok(response);
    }
}

