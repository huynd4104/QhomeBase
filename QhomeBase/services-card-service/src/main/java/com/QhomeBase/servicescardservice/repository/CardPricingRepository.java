package com.QhomeBase.servicescardservice.repository;

import com.QhomeBase.servicescardservice.model.CardPricing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CardPricingRepository extends JpaRepository<CardPricing, UUID> {
    
    Optional<CardPricing> findByCardTypeAndIsActiveTrue(String cardType);
    
    Optional<CardPricing> findByCardType(String cardType);
}

