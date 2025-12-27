package com.QhomeBase.marketplaceservice.repository;

import com.QhomeBase.marketplaceservice.model.MarketplacePost;
import com.QhomeBase.marketplaceservice.model.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
public interface MarketplacePostRepository extends JpaRepository<MarketplacePost, UUID> {

    // Use EntityGraph to avoid Hibernate warning about collection fetch with pagination
    @EntityGraph(attributePaths = {}, type = EntityGraph.EntityGraphType.LOAD)
    Page<MarketplacePost> findByBuildingIdAndStatusOrderByCreatedAtDesc(
        UUID buildingId, 
        PostStatus status, 
        Pageable pageable
    );

    @EntityGraph(attributePaths = {}, type = EntityGraph.EntityGraphType.LOAD)
    Page<MarketplacePost> findByBuildingIdAndStatusAndCategoryOrderByCreatedAtDesc(
        UUID buildingId, 
        PostStatus status, 
        String category, 
        Pageable pageable
    );

    // Use native query with explicit TEXT cast to avoid bytea type issue with LOWER function
    // Note: @EntityGraph doesn't work with native queries, but collections are loaded separately
    @Query(value = "SELECT * FROM marketplace.marketplace_posts p WHERE p.building_id = :buildingId " +
           "AND p.status = CAST(:status AS marketplace.post_status) " +
           "AND (LOWER(p.title) LIKE LOWER('%' || :search || '%') " +
           "OR LOWER(CAST(p.description AS TEXT)) LIKE LOWER('%' || :search || '%')) " +
           "ORDER BY p.created_at DESC",
           countQuery = "SELECT COUNT(*) FROM marketplace.marketplace_posts p WHERE p.building_id = :buildingId " +
           "AND p.status = CAST(:status AS marketplace.post_status) " +
           "AND (LOWER(p.title) LIKE LOWER('%' || :search || '%') " +
           "OR LOWER(CAST(p.description AS TEXT)) LIKE LOWER('%' || :search || '%'))",
           nativeQuery = true)
    Page<MarketplacePost> searchPosts(
        @Param("buildingId") UUID buildingId,
        @Param("status") String status,
        @Param("search") String search,
        Pageable pageable
    );

    // Use native query with explicit TEXT cast to avoid bytea type issue with LOWER function
    // Note: @EntityGraph doesn't work with native queries, but collections are loaded separately
    @Query(value = "SELECT * FROM marketplace.marketplace_posts p WHERE " +
           "(" +
           "  CASE " +
           "    WHEN :filterScope IS NULL OR :filterScope = '' THEN TRUE " +
           "    WHEN :filterScope = 'ALL' THEN (p.scope = CAST('ALL' AS marketplace.post_scope) OR p.scope = CAST('BOTH' AS marketplace.post_scope)) " +
           "    WHEN :filterScope = 'BUILDING' THEN " +
           "      ((p.scope = CAST('BUILDING' AS marketplace.post_scope) AND p.building_id = :buildingId) OR " +
           "       (p.scope = CAST('BOTH' AS marketplace.post_scope) AND p.building_id = :buildingId)) " +
           "    ELSE FALSE " +
           "  END" +
           ") " +
           "AND p.status = CAST(:status AS marketplace.post_status) " +
           "AND (:category IS NULL OR p.category = :category) " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
           "AND (:search IS NULL OR " +
           "     (LOWER(p.title) LIKE LOWER('%' || :search || '%') " +
           "      OR LOWER(CAST(p.description AS TEXT)) LIKE LOWER('%' || :search || '%'))) " +
           "ORDER BY " +
           "CASE WHEN :sortBy = 'newest' THEN p.created_at END DESC NULLS LAST, " +
           "CASE WHEN :sortBy = 'oldest' THEN p.created_at END ASC NULLS LAST, " +
           "CASE WHEN :sortBy = 'price_asc' THEN p.price END ASC NULLS LAST, " +
           "CASE WHEN :sortBy = 'price_desc' THEN p.price END DESC NULLS LAST, " +
           "CASE WHEN :sortBy = 'popular' THEN p.like_count END DESC NULLS LAST, " +
           "p.created_at DESC",
           countQuery = "SELECT COUNT(*) FROM marketplace.marketplace_posts p WHERE " +
           "(" +
           "  CASE " +
           "    WHEN :filterScope IS NULL OR :filterScope = '' THEN TRUE " +
           "    WHEN :filterScope = 'ALL' THEN (p.scope = CAST('ALL' AS marketplace.post_scope) OR p.scope = CAST('BOTH' AS marketplace.post_scope)) " +
           "    WHEN :filterScope = 'BUILDING' THEN " +
           "      ((p.scope = CAST('BUILDING' AS marketplace.post_scope) AND p.building_id = :buildingId) OR " +
           "       (p.scope = CAST('BOTH' AS marketplace.post_scope) AND p.building_id = :buildingId)) " +
           "    ELSE FALSE " +
           "  END" +
           ") " +
           "AND p.status = CAST(:status AS marketplace.post_status) " +
           "AND (:category IS NULL OR p.category = :category) " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
           "AND (:search IS NULL OR " +
           "     (LOWER(p.title) LIKE LOWER('%' || :search || '%') " +
           "      OR LOWER(CAST(p.description AS TEXT)) LIKE LOWER('%' || :search || '%')))",
           nativeQuery = true)
    Page<MarketplacePost> findPostsWithFilters(
        @Param("buildingId") UUID buildingId,
        @Param("status") String status,
        @Param("category") String category,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        @Param("search") String search,
        @Param("sortBy") String sortBy,
        @Param("filterScope") String filterScope,
        Pageable pageable
    );

    // Use EntityGraph to avoid Hibernate warning about collection fetch with pagination
    @EntityGraph(attributePaths = {}, type = EntityGraph.EntityGraphType.LOAD)
    Page<MarketplacePost> findByResidentIdOrderByCreatedAtDesc(
        UUID residentId, 
        Pageable pageable
    );

    @EntityGraph(attributePaths = {}, type = EntityGraph.EntityGraphType.LOAD)
    Page<MarketplacePost> findByResidentIdAndStatusOrderByCreatedAtDesc(
        UUID residentId, 
        PostStatus status, 
        Pageable pageable
    );

    @Query("SELECT COUNT(p) FROM MarketplacePost p WHERE p.buildingId = :buildingId AND p.status = :status")
    Long countByBuildingIdAndStatus(@Param("buildingId") UUID buildingId, @Param("status") PostStatus status);

    // Load post with images to avoid LazyInitializationException
    @Query("SELECT DISTINCT p FROM MarketplacePost p LEFT JOIN FETCH p.images WHERE p.id = :id")
    java.util.Optional<MarketplacePost> findByIdWithImages(@Param("id") UUID id);
}

