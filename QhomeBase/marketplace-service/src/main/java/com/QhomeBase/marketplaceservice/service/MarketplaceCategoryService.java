package com.QhomeBase.marketplaceservice.service;

import com.QhomeBase.marketplaceservice.model.MarketplaceCategory;
import com.QhomeBase.marketplaceservice.repository.MarketplaceCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketplaceCategoryService {

    private final MarketplaceCategoryRepository categoryRepository;
    private final CacheService cacheService;

    /**
     * Get all active categories - cached for 1 hour
     */
    @Cacheable(value = "categories", key = "'all'")
    @Transactional(readOnly = true)
    public List<MarketplaceCategory> getAllActiveCategories() {
        log.debug("Fetching all active categories from database");
        return categoryRepository.findByActiveTrueOrderByDisplayOrderAsc();
    }

    /**
     * Get category by code
     */
    @Transactional(readOnly = true)
    public MarketplaceCategory getCategoryByCode(String code) {
        return categoryRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Category not found: " + code));
    }

    /**
     * Invalidate categories cache (call when categories are updated)
     */
    public void invalidateCategoriesCache() {
        cacheService.evictCategoryCache();
    }
}

