package com.QhomeBase.marketplaceservice.service;

import com.QhomeBase.marketplaceservice.model.MarketplacePost;
import com.QhomeBase.marketplaceservice.model.MarketplacePostImage;
import com.QhomeBase.marketplaceservice.model.PostStatus;
import com.QhomeBase.marketplaceservice.repository.MarketplaceCommentRepository;
import com.QhomeBase.marketplaceservice.repository.MarketplacePostImageRepository;
import com.QhomeBase.marketplaceservice.repository.MarketplacePostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketplacePostService {

    private final MarketplacePostRepository postRepository;
    private final MarketplacePostImageRepository imageRepository;
    private final MarketplaceCommentRepository commentRepository;
    private final CacheService cacheService;
    private final ChatServiceClient chatServiceClient;

    /**
     * Get post by ID - cached for 5 minutes (without block check, for internal use)
     */
    @Cacheable(value = "postDetails", key = "#postId")
    @Transactional(readOnly = true)
    public MarketplacePost getPostById(UUID postId) {
        log.debug("Fetching post from database: {}", postId);
        // Use findByIdWithImages to eager load images and avoid LazyInitializationException
        MarketplacePost post = postRepository.findByIdWithImages(postId)
                .orElseThrow(() -> new RuntimeException("Post not found: " + postId));
        
        // Sync comment count with actual count from database to ensure accuracy
        Long actualCommentCount = commentRepository.countActiveCommentsByPostId(postId);
        if (actualCommentCount != null && !actualCommentCount.equals(post.getCommentCount())) {
            log.warn("Comment count mismatch for post {}: stored={}, actual={}. Syncing...", 
                    postId, post.getCommentCount(), actualCommentCount);
            // Note: We don't update here in read-only transaction, but log the mismatch
            // The count will be corrected on next write operation
        }
        
        return post;
    }

    /**
     * Get post by ID with block check
     * Note: Not cached when currentResidentId is provided to ensure block status is always checked
     */
    @Cacheable(value = "postDetails", key = "#postId + '_' + (#currentResidentId != null ? #currentResidentId : 'no-user')", condition = "#currentResidentId == null")
    @Transactional(readOnly = true)
    public MarketplacePost getPostById(UUID postId, UUID currentResidentId, String accessToken) {
        log.debug("Fetching post from database: {}", postId);
        // Use findByIdWithImages to eager load images and avoid LazyInitializationException
        MarketplacePost post = postRepository.findByIdWithImages(postId)
                .orElseThrow(() -> new RuntimeException("Post not found: " + postId));
        
        // Check if current user is blocked from viewing this post
        if (currentResidentId != null && accessToken != null && !accessToken.isEmpty()) {
            log.info("🔍 [MarketplacePostService] Checking block for post {} - currentResidentId: {}, post author: {}", 
                    postId, currentResidentId, post.getResidentId());
            try {
                // Get users blocked by current user (A block B -> B won't see A's posts)
                List<UUID> blockedUserIds = chatServiceClient.getBlockedUserIds(accessToken);
                log.info("🔍 [MarketplacePostService] Got {} blocked users (residentIds): {}", blockedUserIds.size(), blockedUserIds);
                
                // Get users who blocked current user (B block A -> A won't see B's posts)
                List<UUID> blockedByUserIds = chatServiceClient.getBlockedByUserIds(accessToken);
                log.info("🔍 [MarketplacePostService] Got {} blocked-by users (residentIds): {}", blockedByUserIds.size(), blockedByUserIds);
                
                // Combine both lists for checking
                java.util.Set<UUID> allBlockedUserIds = new java.util.HashSet<>();
                allBlockedUserIds.addAll(blockedUserIds);
                allBlockedUserIds.addAll(blockedByUserIds);
                
                log.info("🔍 [MarketplacePostService] All blocked residentIds: {}", allBlockedUserIds);
                log.info("🔍 [MarketplacePostService] Post author residentId: {}", post.getResidentId());
                log.info("🔍 [MarketplacePostService] Is post author blocked? {}", allBlockedUserIds.contains(post.getResidentId()));
                
                // Check if post author is in blocked list (either direction)
                if (allBlockedUserIds.contains(post.getResidentId())) {
                    log.info("🚫 [MarketplacePostService] Post {} is blocked - author {} is in blocked list for user {}", 
                            postId, post.getResidentId(), currentResidentId);
                    throw new RuntimeException("Post not found: " + postId); // Return same error as if post doesn't exist
                } else {
                    log.info("✅ [MarketplacePostService] Post {} is NOT blocked - author {} is not in blocked list", 
                            postId, post.getResidentId());
                }
            } catch (RuntimeException e) {
                // Re-throw if it's our "not found" exception
                if (e.getMessage() != null && e.getMessage().contains("Post not found")) {
                    throw e;
                }
                log.error("❌ [MarketplacePostService] RuntimeException checking blocked users for post {}: {}", postId, e.getMessage(), e);
                // Continue with post if error occurs (fail open)
            } catch (Exception e) {
                log.error("❌ [MarketplacePostService] Error checking blocked users for post {}: {}", postId, e.getMessage(), e);
                e.printStackTrace();
                // Continue with post if error occurs (fail open)
            }
        } else {
            log.info("🔍 [MarketplacePostService] Skipping block check for post {} - currentResidentId: {}, accessToken present: {}", 
                    postId, currentResidentId, accessToken != null && !accessToken.isEmpty());
        }
        
        // Sync comment count with actual count from database to ensure accuracy
        Long actualCommentCount = commentRepository.countActiveCommentsByPostId(postId);
        if (actualCommentCount != null && !actualCommentCount.equals(post.getCommentCount())) {
            log.warn("Comment count mismatch for post {}: stored={}, actual={}. Syncing...", 
                    postId, post.getCommentCount(), actualCommentCount);
            // Note: We don't update here in read-only transaction, but log the mismatch
            // The count will be corrected on next write operation
        }
        
        return post;
    }

    /**
     * Get posts with filters
     * Note: Not cached when currentResidentId is provided to ensure block status is always checked
     */
    @Cacheable(value = "postList", key = "(#buildingId != null ? #buildingId : 'all') + '_' + #status + '_' + (#category != null ? #category : 'all') + '_' + #page + '_' + #size + '_' + (#sortBy != null ? #sortBy : 'newest') + '_' + (#filterScope != null ? #filterScope : 'null') + '_' + (#currentResidentId != null ? #currentResidentId : 'no-user')", condition = "#currentResidentId == null")
    @Transactional(readOnly = true)
    public Page<MarketplacePost> getPosts(
            UUID buildingId,
            PostStatus status,
            String category,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String search,
            String sortBy,
            String filterScope,
            UUID currentResidentId,
            String accessToken,
            int page,
            int size) {
        
        log.info("🔍 [MarketplacePostService] Fetching posts: buildingId={}, status={}, category={}, filterScope={}, page={}, size={}", 
                buildingId, status, category, filterScope, page, size);

        // Default sort
        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "newest";
        }

        Pageable pageable = PageRequest.of(page, size);
        
        Page<MarketplacePost> posts = postRepository.findPostsWithFilters(
                buildingId, status.name(), category, minPrice, maxPrice, search, sortBy, filterScope, pageable
        );
        
        log.info("🔍 [MarketplacePostService] Found {} posts (total: {})", posts.getContent().size(), posts.getTotalElements());
        
        // Load images for all posts in batch FIRST to avoid N+1 problem and LazyInitializationException
        // This must be done before filtering to ensure images are loaded while session is still open
        List<MarketplacePost> postsToProcess = posts.getContent();
        if (!postsToProcess.isEmpty()) {
            List<UUID> postIds = postsToProcess.stream()
                    .map(MarketplacePost::getId)
                    .toList();
            
            // Load all images for these posts in one query
            List<MarketplacePostImage> allImages = imageRepository.findByPostIdIn(postIds);
            
            // Group images by post ID and set them to posts
            java.util.Map<UUID, List<MarketplacePostImage>> imagesByPostId = allImages.stream()
                    .collect(java.util.stream.Collectors.groupingBy(img -> img.getPost().getId()));
            
            // Set images to each post - create new ArrayList to replace lazy proxy
            postsToProcess.forEach(post -> {
                List<MarketplacePostImage> postImages = imagesByPostId.getOrDefault(
                        post.getId(), 
                        new java.util.ArrayList<>()
                );
                // Create a new ArrayList to ensure we're not keeping lazy proxy
                // This forces initialization and prevents LazyInitializationException
                post.setImages(new java.util.ArrayList<>(postImages));
            });
        }
        
        // Filter out posts from blocked users if currentResidentId is provided
        // This is done AFTER loading images to ensure images are already loaded
        if (currentResidentId != null && accessToken != null && !accessToken.isEmpty()) {
            log.info("🔍 [MarketplacePostService] Checking blocked users for currentResidentId: {}, posts count: {}", 
                    currentResidentId, postsToProcess.size());
            try {
                // Get users blocked by current user (A block B -> B won't see A's posts)
                List<UUID> blockedUserIds = chatServiceClient.getBlockedUserIds(accessToken);
                log.info("🔍 [MarketplacePostService] Got {} blocked users from chat-service", blockedUserIds.size());
                if (!blockedUserIds.isEmpty()) {
                    log.info("🔍 [MarketplacePostService] Blocked user IDs: {}", blockedUserIds);
                }
                
                // Get users who blocked current user (B block A -> A won't see B's posts)
                List<UUID> blockedByUserIds = chatServiceClient.getBlockedByUserIds(accessToken);
                log.info("🔍 [MarketplacePostService] Got {} blocked-by users from chat-service", blockedByUserIds.size());
                if (!blockedByUserIds.isEmpty()) {
                    log.info("🔍 [MarketplacePostService] Blocked-by user IDs: {}", blockedByUserIds);
                }
                
                // Combine both lists for filtering
                java.util.Set<UUID> allBlockedUserIds = new java.util.HashSet<>();
                allBlockedUserIds.addAll(blockedUserIds);
                allBlockedUserIds.addAll(blockedByUserIds);
                
                log.info("🔍 [MarketplacePostService] Total blocked users (combined): {}", allBlockedUserIds.size());
                if (!allBlockedUserIds.isEmpty()) {
                    log.info("🔍 [MarketplacePostService] All blocked user IDs: {}", allBlockedUserIds);
                    log.info("🔍 [MarketplacePostService] Filtering posts from {} blocked users (blocked: {}, blocked-by: {})", 
                            allBlockedUserIds.size(), blockedUserIds.size(), blockedByUserIds.size());
                    
                    // Log all post authors before filtering
                    log.info("🔍 [MarketplacePostService] Post authors before filtering: {}", 
                            postsToProcess.stream().map(MarketplacePost::getResidentId).collect(java.util.stream.Collectors.toList()));
                    
                    // Filter posts: remove posts from blocked users (both directions)
                    List<MarketplacePost> filteredPosts = postsToProcess.stream()
                            .filter(post -> {
                                // Check if post author is in blocked list (either direction)
                                boolean isBlocked = allBlockedUserIds.contains(post.getResidentId());
                                if (isBlocked) {
                                    log.info("🚫 [MarketplacePostService] Filtering post {} - author {} is blocked (bidirectional check)", 
                                            post.getId(), post.getResidentId());
                                }
                                return !isBlocked;
                            })
                            .collect(java.util.stream.Collectors.toList());
                    
                    // Create a new Page with filtered content (images already loaded)
                    if (filteredPosts.size() != postsToProcess.size()) {
                        log.info("🔍 [MarketplacePostService] Filtered {} posts (removed {} blocked posts)", 
                                filteredPosts.size(), postsToProcess.size() - filteredPosts.size());
                        
                        // Create a new Page with filtered content
                        // Note: This changes the total count, so we need to recalculate
                        return new org.springframework.data.domain.PageImpl<>(
                                filteredPosts,
                                pageable,
                                posts.getTotalElements() - (postsToProcess.size() - filteredPosts.size())
                        );
                    } else {
                        log.info("🔍 [MarketplacePostService] No posts were filtered (all posts are visible)");
                    }
                } else {
                    log.info("🔍 [MarketplacePostService] No blocked users found, returning all posts");
                }
            } catch (Exception e) {
                log.error("❌ [MarketplacePostService] Error filtering posts by blocked users: {}", e.getMessage(), e);
                e.printStackTrace();
                // Continue with unfiltered posts if error occurs
            }
        } else {
            log.info("🔍 [MarketplacePostService] Skipping block check - currentResidentId: {}, accessToken present: {}", 
                    currentResidentId, accessToken != null && !accessToken.isEmpty());
        }
        
        return posts;
    }

    /**
     * Get popular posts
     * Note: Not cached when currentResidentId is provided to ensure block status is always checked
     */
    @Cacheable(value = "popularPosts", key = "#buildingId + '_' + #page + '_' + (#currentResidentId != null ? #currentResidentId : 'no-user')", condition = "#currentResidentId == null")
    @Transactional(readOnly = true)
    public Page<MarketplacePost> getPopularPosts(UUID buildingId, UUID currentResidentId, String accessToken, int page, int size) {
        log.debug("Fetching popular posts from database: buildingId={}, page={}", buildingId, page);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "likeCount", "createdAt"));
        Page<MarketplacePost> posts = postRepository.findByBuildingIdAndStatusOrderByCreatedAtDesc(buildingId, PostStatus.ACTIVE, pageable);
        
        // Load images first
        List<MarketplacePost> postsToProcess = posts.getContent();
        if (!postsToProcess.isEmpty()) {
            List<UUID> postIds = postsToProcess.stream()
                    .map(MarketplacePost::getId)
                    .toList();
            
            List<MarketplacePostImage> allImages = imageRepository.findByPostIdIn(postIds);
            java.util.Map<UUID, List<MarketplacePostImage>> imagesByPostId = allImages.stream()
                    .collect(java.util.stream.Collectors.groupingBy(img -> img.getPost().getId()));
            
            postsToProcess.forEach(post -> {
                List<MarketplacePostImage> postImages = imagesByPostId.getOrDefault(
                        post.getId(), 
                        new java.util.ArrayList<>()
                );
                post.setImages(new java.util.ArrayList<>(postImages));
            });
        }
        
        // Filter out posts from blocked users if currentResidentId is provided
        if (currentResidentId != null && accessToken != null && !accessToken.isEmpty()) {
            try {
                List<UUID> blockedUserIds = chatServiceClient.getBlockedUserIds(accessToken);
                List<UUID> blockedByUserIds = chatServiceClient.getBlockedByUserIds(accessToken);
                java.util.Set<UUID> allBlockedUserIds = new java.util.HashSet<>();
                allBlockedUserIds.addAll(blockedUserIds);
                allBlockedUserIds.addAll(blockedByUserIds);
                
                if (!allBlockedUserIds.isEmpty()) {
                    List<MarketplacePost> filteredPosts = postsToProcess.stream()
                            .filter(post -> !allBlockedUserIds.contains(post.getResidentId()))
                            .collect(java.util.stream.Collectors.toList());
                    
                    if (filteredPosts.size() != postsToProcess.size()) {
                        return new org.springframework.data.domain.PageImpl<>(
                                filteredPosts,
                                pageable,
                                posts.getTotalElements() - (postsToProcess.size() - filteredPosts.size())
                        );
                    }
                }
            } catch (Exception e) {
                log.error("Error filtering popular posts by blocked users: {}", e.getMessage(), e);
            }
        }
        
        return posts;
    }

    /**
     * Get my posts
     */
    @Transactional(readOnly = true)
    public Page<MarketplacePost> getMyPosts(UUID residentId, PostStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (status != null) {
            return postRepository.findByResidentIdAndStatusOrderByCreatedAtDesc(residentId, status, pageable);
        }
        return postRepository.findByResidentIdOrderByCreatedAtDesc(residentId, pageable);
    }

    /**
     * Create post - evicts cache
     */
    @CacheEvict(value = {"postList", "popularPosts"}, allEntries = true)
    @Transactional
    public MarketplacePost createPost(MarketplacePost post) {
        log.info("Creating new post: {}", post.getTitle());
        log.info("📝 [MarketplacePostService] Post contactInfo before save: {}", post.getContactInfo());
        MarketplacePost saved = postRepository.save(post);
        log.info("📝 [MarketplacePostService] Post contactInfo after save: {}", saved.getContactInfo());
        // Also evict post details cache for this post
        cacheService.evictPostCaches(saved.getId());
        return saved;
    }

    /**
     * Update post - evicts cache
     */
    @CacheEvict(value = {"postDetails", "postList", "popularPosts"}, allEntries = true)
    @Transactional
    public MarketplacePost updatePost(UUID postId, MarketplacePost post) {
        log.info("Updating post: {}", postId);
        if (!postRepository.existsById(postId)) {
            throw new RuntimeException("Post not found: " + postId);
        }
        post.setId(postId);
        MarketplacePost saved = postRepository.save(post);
        cacheService.evictPostCaches(saved.getId());
        return saved;
    }

    /**
     * Delete post - evicts cache
     */
    @CacheEvict(value = {"postDetails", "postList", "popularPosts"}, allEntries = true)
    @Transactional
    public void deletePost(UUID postId) {
        log.info("Deleting post: {}", postId);
        postRepository.deleteById(postId);
        cacheService.evictPostCaches(postId);
    }

    /**
     * Update post status - evicts cache
     */
    @CacheEvict(value = {"postDetails", "postList", "popularPosts"}, allEntries = true)
    @Transactional
    public MarketplacePost updatePostStatus(UUID postId, PostStatus status) {
        log.info("Updating post status: {} -> {}", postId, status);
        MarketplacePost post = getPostById(postId);
        post.setStatus(status);
        MarketplacePost saved = postRepository.save(post);
        cacheService.evictPostCaches(saved.getId());
        return saved;
    }

    /**
     * Increment view count
     */
    @Transactional
    public void incrementViewCount(UUID postId) {
        MarketplacePost post = getPostById(postId);
        post.incrementViewCount();
        postRepository.save(post);
        // Evict cache to reflect new view count
        cacheService.evictCacheEntry("postDetails", postId);
    }
}

