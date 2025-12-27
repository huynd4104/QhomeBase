package com.QhomeBase.marketplaceservice.repository;

import com.QhomeBase.marketplaceservice.model.MarketplaceComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MarketplaceCommentRepository extends JpaRepository<MarketplaceComment, UUID> {

    @Query("SELECT c FROM MarketplaceComment c WHERE c.post.id = :postId AND c.parentComment IS NULL AND c.deletedAt IS NULL ORDER BY c.createdAt ASC")
    @EntityGraph(attributePaths = {"replies"})
    List<MarketplaceComment> findByPostIdAndParentCommentIsNullOrderByCreatedAtAsc(@Param("postId") UUID postId);

    @Query("SELECT c FROM MarketplaceComment c WHERE c.post.id = :postId AND c.parentComment IS NULL AND c.deletedAt IS NULL ORDER BY c.createdAt ASC")
    @EntityGraph(attributePaths = {"replies"})
    Page<MarketplaceComment> findByPostIdAndParentCommentIsNullOrderByCreatedAtAsc(@Param("postId") UUID postId, Pageable pageable);

    List<MarketplaceComment> findByParentCommentIdOrderByCreatedAtAsc(UUID parentCommentId);

    Long countByPostId(UUID postId);

    @Query("SELECT COUNT(c) FROM MarketplaceComment c WHERE c.post.id = :postId AND c.deletedAt IS NULL AND c.parentComment IS NULL")
    Long countActiveTopLevelCommentsByPostId(@Param("postId") UUID postId);
    
    @Query("SELECT COUNT(c) FROM MarketplaceComment c WHERE c.post.id = :postId AND c.deletedAt IS NULL")
    Long countActiveCommentsByPostId(@Param("postId") UUID postId);
}

