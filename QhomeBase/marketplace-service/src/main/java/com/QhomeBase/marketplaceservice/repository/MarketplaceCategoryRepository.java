package com.QhomeBase.marketplaceservice.repository;

import com.QhomeBase.marketplaceservice.model.MarketplaceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MarketplaceCategoryRepository extends JpaRepository<MarketplaceCategory, UUID> {

    Optional<MarketplaceCategory> findByCode(String code);

    List<MarketplaceCategory> findByActiveTrueOrderByDisplayOrderAsc();

    boolean existsByCode(String code);
}

