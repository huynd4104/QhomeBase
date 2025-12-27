package com.QhomeBase.marketplaceservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service for manual cache operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {

    private final CacheManager cacheManager;

    /**
     * Evict all entries from a specific cache
     */
    public void evictCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            log.debug("Cleared cache: {}", cacheName);
        }
    }

    /**
     * Evict a specific entry from a cache
     */
    public void evictCacheEntry(String cacheName, Object key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
            log.debug("Evicted cache entry: {} -> {}", cacheName, key);
        }
    }

    /**
     * Evict all post-related caches
     */
    public void evictPostCaches(UUID postId) {
        evictCacheEntry("postDetails", postId);
        evictCache("popularPosts");
        evictCache("postList");
        log.info("Evicted all caches for post: {}", postId);
    }

    /**
     * Evict category cache
     */
    public void evictCategoryCache() {
        evictCache("categories");
        log.info("Evicted categories cache");
    }

    /**
     * Evict user likes cache for a specific post
     */
    public void evictUserLikesCache(UUID postId, UUID residentId) {
        String key = postId + "_" + residentId;
        evictCacheEntry("userLikes", key);
        log.debug("Evicted user likes cache: {}", key);
    }

    /**
     * Evict resident info cache
     */
    public void evictResidentInfoCache() {
        evictCache("residentInfo");
        log.info("Evicted residentInfo cache");
    }
}

