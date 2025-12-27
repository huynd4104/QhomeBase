package com.QhomeBase.marketplaceservice.service;

import com.QhomeBase.marketplaceservice.model.MarketplacePost;
import com.QhomeBase.marketplaceservice.model.PostStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Service for feed algorithm - sorts posts by relevance, popularity, trending
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeedAlgorithmService {

    private final MarketplacePostService postService;

    /**
     * Get feed posts sorted by algorithm
     * Algorithm considers:
     * - Recency (newer posts get boost)
     * - Popularity (likes, comments, views)
     * - Trending (recent activity)
     */
    @Transactional(readOnly = true)
    public Page<MarketplacePost> getFeedPosts(
            UUID buildingId,
            String sortBy,
            int page,
            int size) {
        
        // Default to "newest" if not specified
        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "newest";
        }

        // For now, use existing service methods
        // In future, can implement more sophisticated algorithm
        switch (sortBy.toLowerCase()) {
            case "popular":
                return postService.getPopularPosts(buildingId, null, null, page, size);
            case "trending":
                // Trending = posts with high engagement in last 24 hours
                // For now, use popular as fallback
                return postService.getPopularPosts(buildingId, null, null, page, size);
            case "newest":
            default:
                return postService.getPosts(
                        buildingId,
                        PostStatus.ACTIVE,
                        null, // category
                        null, // minPrice
                        null, // maxPrice
                        null, // search
                        "newest",
                        null, // filterScope
                        null, // currentResidentId - FeedAlgorithmService doesn't have access to auth context
                        null, // accessToken - FeedAlgorithmService doesn't have access to auth context
                        page,
                        size
                );
        }
    }

    /**
     * Calculate relevance score for a post
     * Higher score = more relevant
     */
    private double calculateRelevanceScore(MarketplacePost post) {
        double score = 0.0;
        
        // Recency boost (newer posts get higher score)
        OffsetDateTime now = OffsetDateTime.now();
        long hoursSinceCreation = java.time.Duration.between(post.getCreatedAt(), now).toHours();
        double recencyScore = Math.max(0, 100 - hoursSinceCreation * 2); // Decay over time
        score += recencyScore * 0.3; // 30% weight
        
        // Popularity boost (likes and comments)
        double popularityScore = (post.getLikeCount() * 2 + post.getCommentCount()) * 10;
        score += popularityScore * 0.4; // 40% weight
        
        // View boost (but less than likes/comments)
        double viewScore = post.getViewCount() * 0.1;
        score += viewScore * 0.2; // 20% weight
        
        // Recent activity boost (if post has recent comments/likes)
        // This would require additional queries in real implementation
        score += 10 * 0.1; // 10% weight
        
        return score;
    }
}

