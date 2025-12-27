package com.QhomeBase.marketplaceservice.repository;

import com.QhomeBase.marketplaceservice.model.MarketplaceLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MarketplaceLikeRepository extends JpaRepository<MarketplaceLike, UUID> {

    Optional<MarketplaceLike> findByPostIdAndResidentId(UUID postId, UUID residentId);

    boolean existsByPostIdAndResidentId(UUID postId, UUID residentId);

    Long countByPostId(UUID postId);
}

