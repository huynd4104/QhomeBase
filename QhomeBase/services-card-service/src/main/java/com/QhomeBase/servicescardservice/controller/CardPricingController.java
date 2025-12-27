package com.QhomeBase.servicescardservice.controller;

import com.QhomeBase.servicescardservice.dto.CardPricingDto;
import com.QhomeBase.servicescardservice.dto.CreateCardPricingRequest;
import com.QhomeBase.servicescardservice.model.CardPricing;
import com.QhomeBase.servicescardservice.service.CardPricingService;
import com.QhomeBase.servicescardservice.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/card-pricing")
@RequiredArgsConstructor
@Slf4j
public class CardPricingController {

    private final CardPricingService cardPricingService;
    private final JwtUtil jwtUtil;

    /**
     * Get all card pricing configurations
     */
    @GetMapping
    public ResponseEntity<List<CardPricingDto>> getAllPricing() {
        List<CardPricing> pricingList = cardPricingService.getAllPricing();
        List<CardPricingDto> dtos = pricingList.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get pricing by card type
     */
    @GetMapping("/type/{cardType}")
    public ResponseEntity<CardPricingDto> getPricingByCardType(@PathVariable String cardType) {
        CardPricing pricing = cardPricingService.getPricingByCardType(cardType.toUpperCase());
        return ResponseEntity.ok(toDto(pricing));
    }

    /**
     * Get current active price for a card type (for public use)
     */
    @GetMapping("/type/{cardType}/price")
    public ResponseEntity<CardPricingDto> getActivePrice(@PathVariable String cardType) {
        BigDecimal price = cardPricingService.getPrice(cardType.toUpperCase());
        CardPricingDto dto = CardPricingDto.builder()
                .cardType(cardType.toUpperCase())
                .price(price)
                .currency("VND")
                .build();
        return ResponseEntity.ok(dto);
    }

    /**
     * Create or update card pricing (admin only)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createOrUpdatePricing(
            @Valid @RequestBody CreateCardPricingRequest request,
            @RequestHeader HttpHeaders headers) {
        
        UUID adminId = jwtUtil.getUserIdFromHeaders(headers);
        if (adminId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            log.info("📥 [CardPricingController] createOrUpdatePricing: cardType={}, price={}, adminId={}", 
                    request.getCardType(), request.getPrice(), adminId);
            
            CardPricing pricing = CardPricing.builder()
                    .cardType(request.getCardType().toUpperCase())
                    .price(request.getPrice())
                    .currency(request.getCurrency() != null ? request.getCurrency() : "VND")
                    .description(request.getDescription())
                    .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                    .createdBy(adminId)
                    .updatedBy(adminId)
                    .build();
            
            CardPricing saved = cardPricingService.savePricing(pricing);
            return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
        } catch (IllegalArgumentException ex) {
            log.error("❌ [CardPricingController] Validation error - cardType: {}, price: {}, error: {}", 
                    request.getCardType(), request.getPrice(), ex.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
            errorResponse.put("error", "Bad Request");
            errorResponse.put("message", ex.getMessage());
            errorResponse.put("timestamp", java.time.OffsetDateTime.now());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    private CardPricingDto toDto(CardPricing pricing) {
        return CardPricingDto.builder()
                .id(pricing.getId())
                .cardType(pricing.getCardType())
                .price(pricing.getPrice())
                .currency(pricing.getCurrency())
                .description(pricing.getDescription())
                .isActive(pricing.getIsActive())
                .createdAt(pricing.getCreatedAt())
                .createdBy(pricing.getCreatedBy())
                .updatedAt(pricing.getUpdatedAt())
                .updatedBy(pricing.getUpdatedBy())
                .build();
    }
}

