package com.QhomeBase.marketplaceservice.repository;

import com.QhomeBase.marketplaceservice.model.MarketplacePostImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MarketplacePostImageRepository extends JpaRepository<MarketplacePostImage, UUID> {
    
    List<MarketplacePostImage> findByPostIdOrderBySortOrderAsc(UUID postId);
    
    // Removed JOIN FETCH to avoid Hibernate warning when used with pagination
    // The post relationship is already loaded from the parent query
    @Query("SELECT i FROM MarketplacePostImage i WHERE i.post.id IN :postIds ORDER BY i.post.id, i.sortOrder ASC")
    List<MarketplacePostImage> findByPostIdIn(@Param("postIds") List<UUID> postIds);
}

