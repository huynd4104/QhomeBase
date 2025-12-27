package com.QhomeBase.servicescardservice.service;

import com.QhomeBase.servicescardservice.model.CardPricing;
import com.QhomeBase.servicescardservice.repository.CardPricingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardPricingService {

    private final CardPricingRepository cardPricingRepository;

    /**
     * Get active pricing for a card type
     * Returns default 30000 if not found
     */
    public BigDecimal getPrice(String cardType) {
        return cardPricingRepository.findByCardTypeAndIsActiveTrue(cardType)
                .map(CardPricing::getPrice)
                .orElseGet(() -> {
                    log.warn("⚠️ [CardPricingService] No active pricing found for card type: {}, using default 30000", cardType);
                    return BigDecimal.valueOf(30000);
                });
    }

    /**
     * Get all card pricing configurations
     */
    public List<CardPricing> getAllPricing() {
        return cardPricingRepository.findAll();
    }

    /**
     * Get pricing by card type (including inactive)
     */
    public CardPricing getPricingByCardType(String cardType) {
        return cardPricingRepository.findByCardType(cardType)
                .orElseThrow(() -> new IllegalArgumentException("Card pricing not found for type: " + cardType));
    }

    /**
     * Create or update card pricing
     */
    @Transactional
    public CardPricing savePricing(CardPricing pricing) {
        // Validate card type
        String cardType = pricing.getCardType();
        
        if (!isValidCardType(cardType)) {
            String errorMsg = String.format(
                "Invalid card type: '%s'. Valid card types are: VEHICLE, RESIDENT, ELEVATOR", 
                cardType
            );
            log.error("❌ [CardPricingService] Validation failed - cardType: '{}', error: {}", cardType, errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        // Validate price
        if (pricing.getPrice() == null || pricing.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            String errorMsg = String.format(
                "Price must be greater than 0. Received: %s", 
                pricing.getPrice()
            );
            log.error("❌ [CardPricingService] Validation failed - price: {}, error: {}", pricing.getPrice(), errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        // Check if pricing exists for this card type
        Optional<CardPricing> existing = cardPricingRepository.findByCardType(cardType);
        if (existing.isPresent()) {
            CardPricing existingPricing = existing.get();
            // Preserve createdBy from existing record when updating
            // Only update createdBy if it's null (for backward compatibility with old records from migration)
            if (existingPricing.getCreatedBy() == null && pricing.getCreatedBy() != null) {
                existingPricing.setCreatedBy(pricing.getCreatedBy());
            }
            existingPricing.setPrice(pricing.getPrice());
            existingPricing.setCurrency(pricing.getCurrency() != null ? pricing.getCurrency() : "VND");
            existingPricing.setDescription(pricing.getDescription());
            existingPricing.setIsActive(pricing.getIsActive() != null ? pricing.getIsActive() : true);
            existingPricing.setUpdatedBy(pricing.getUpdatedBy());
            return cardPricingRepository.save(existingPricing);
        } else {
            // New record - ensure createdBy is set
            if (pricing.getCurrency() == null || pricing.getCurrency().isEmpty()) {
                pricing.setCurrency("VND");
            }
            if (pricing.getIsActive() == null) {
                pricing.setIsActive(true);
            }
            // Ensure createdBy is set for new records (use updatedBy if createdBy is null)
            if (pricing.getCreatedBy() == null && pricing.getUpdatedBy() != null) {
                pricing.setCreatedBy(pricing.getUpdatedBy());
            }
            return cardPricingRepository.save(pricing);
        }
    }

    /**
     * Update pricing
     */
    @Transactional
    public CardPricing updatePricing(UUID id, CardPricing pricing) {
        CardPricing existing = cardPricingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Card pricing not found with ID: " + id));

        if (pricing.getPrice() != null) {
            if (pricing.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Price must be greater than 0");
            }
            existing.setPrice(pricing.getPrice());
        }

        if (pricing.getCurrency() != null) {
            existing.setCurrency(pricing.getCurrency());
        }

        if (pricing.getDescription() != null) {
            existing.setDescription(pricing.getDescription());
        }

        if (pricing.getIsActive() != null) {
            existing.setIsActive(pricing.getIsActive());
        }

        if (pricing.getUpdatedBy() != null) {
            existing.setUpdatedBy(pricing.getUpdatedBy());
        }

        return cardPricingRepository.save(existing);
    }

    /**
     * Delete pricing (soft delete by setting is_active = false)
     */
    @Transactional
    public void deletePricing(UUID id, UUID deletedBy) {
        CardPricing pricing = cardPricingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Card pricing not found with ID: " + id));
        
        pricing.setIsActive(false);
        pricing.setUpdatedBy(deletedBy);
        cardPricingRepository.save(pricing);
    }

    private boolean isValidCardType(String cardType) {
        return cardType != null && 
               (cardType.equals("VEHICLE") || cardType.equals("RESIDENT") || cardType.equals("ELEVATOR"));
    }
}

