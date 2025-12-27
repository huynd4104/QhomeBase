package com.QhomeBase.marketplaceservice.service;

import com.QhomeBase.marketplaceservice.model.MarketplaceLike;
import com.QhomeBase.marketplaceservice.model.MarketplacePost;
import com.QhomeBase.marketplaceservice.repository.MarketplaceLikeRepository;
import com.QhomeBase.marketplaceservice.repository.MarketplacePostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketplaceLikeService {

    private final MarketplaceLikeRepository likeRepository;
    private final MarketplacePostRepository postRepository;
    private final CacheService cacheService;
    private final MarketplaceNotificationService notificationService;

    /**
     * Check if user liked a post - cached for 10 minutes
     */
    @Cacheable(value = "userLikes", key = "#postId + '_' + #residentId")
    @Transactional(readOnly = true)
    public boolean isLikedByUser(UUID postId, UUID residentId) {
        log.debug("Checking if post {} is liked by user {}", postId, residentId);
        return likeRepository.existsByPostIdAndResidentId(postId, residentId);
    }

    /**
     * Toggle like - evicts cache
     */
    @CacheEvict(value = {"userLikes", "postDetails", "popularPosts"}, allEntries = true)
    @Transactional
    public boolean toggleLike(UUID postId, UUID residentId) {
        log.info("Toggling like for post {} by user {}", postId, residentId);
        
        MarketplacePost post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found: " + postId));

        return likeRepository.findByPostIdAndResidentId(postId, residentId)
                .map(like -> {
                    // Unlike
                    likeRepository.delete(like);
                    post.decrementLikeCount();
                    postRepository.save(post);
                    cacheService.evictUserLikesCache(postId, residentId);
                    notificationService.notifyNewLike(postId, residentId, "User", false);
                    log.info("User {} unliked post {}", residentId, postId);
                    return false;
                })
                .orElseGet(() -> {
                    // Like
                    MarketplaceLike like = MarketplaceLike.builder()
                            .post(post)
                            .residentId(residentId)
                            .build();
                    likeRepository.save(like);
                    post.incrementLikeCount();
                    postRepository.save(post);
                    cacheService.evictUserLikesCache(postId, residentId);
                    notificationService.notifyNewLike(postId, residentId, "User", true);
                    log.info("User {} liked post {}", residentId, postId);
                    return true;
                });
    }

    /**
     * Get like count for a post
     */
    @Transactional(readOnly = true)
    public Long getLikeCount(UUID postId) {
        return likeRepository.countByPostId(postId);
    }
}

