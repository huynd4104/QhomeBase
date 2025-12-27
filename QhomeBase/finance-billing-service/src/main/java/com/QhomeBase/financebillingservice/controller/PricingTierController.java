package com.QhomeBase.financebillingservice.controller;

import com.QhomeBase.financebillingservice.dto.CreatePricingTierRequest;
import com.QhomeBase.financebillingservice.dto.PricingTierDto;
import com.QhomeBase.financebillingservice.dto.UpdatePricingTierRequest;
import com.QhomeBase.financebillingservice.service.PricingTierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pricing-tiers")
@RequiredArgsConstructor
@Slf4j
public class PricingTierController {
    
    private final PricingTierService pricingTierService;
    
    @PostMapping
    public ResponseEntity<PricingTierDto> createPricingTier(
            @Valid @RequestBody CreatePricingTierRequest request,
            @RequestParam(required = false) UUID createdBy) {
        PricingTierDto tier = pricingTierService.createPricingTier(request, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(tier);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<PricingTierDto> getPricingTierById(@PathVariable UUID id) {
        PricingTierDto tier = pricingTierService.getById(id);
        return ResponseEntity.ok(tier);
    }
    
    @GetMapping
    public ResponseEntity<List<PricingTierDto>> getAllPricingTiers(
            @RequestParam(required = false) String serviceCode) {
        List<PricingTierDto> tiers;
        if (serviceCode != null && !serviceCode.isEmpty()) {
            tiers = pricingTierService.getAllPricingTiers(serviceCode);
        } else {
            tiers = List.of();
        }
        return ResponseEntity.ok(tiers);
    }
    
    @GetMapping("/service/{serviceCode}")
    public ResponseEntity<List<PricingTierDto>> getPricingTiersByServiceCode(
            @PathVariable String serviceCode) {
        List<PricingTierDto> tiers = pricingTierService.getAllPricingTiers(serviceCode);
        return ResponseEntity.ok(tiers);
    }
    
    @GetMapping("/service/{serviceCode}/active")
    public ResponseEntity<List<PricingTierDto>> getActiveTiersByServiceAndDate(
            @PathVariable String serviceCode,
            @RequestParam LocalDate date) {
        List<PricingTierDto> tiers = pricingTierService.getActiveTiersByServiceAndDate(serviceCode, date);
        return ResponseEntity.ok(tiers);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<PricingTierDto> updatePricingTier(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePricingTierRequest request,
            @RequestParam(required = false) UUID updatedBy) {
        PricingTierDto tier = pricingTierService.updatePricingTier(id, request, updatedBy);
        return ResponseEntity.ok(tier);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePricingTier(@PathVariable UUID id) {
        pricingTierService.deletePricingTier(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/service/{serviceCode}/last-order")
    public ResponseEntity<Integer> getLastOrder(@PathVariable String serviceCode) {
        Integer lastOrder = pricingTierService.getLastOrder(serviceCode);
        return ResponseEntity.ok(lastOrder);
    }
}

