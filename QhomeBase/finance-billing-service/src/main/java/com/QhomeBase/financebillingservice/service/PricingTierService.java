package com.QhomeBase.financebillingservice.service;

import com.QhomeBase.financebillingservice.dto.CreateInvoiceLineRequest;
import com.QhomeBase.financebillingservice.dto.CreatePricingTierRequest;
import com.QhomeBase.financebillingservice.dto.PricingTierDto;
import com.QhomeBase.financebillingservice.dto.UpdatePricingTierRequest;
import com.QhomeBase.financebillingservice.model.PricingTier;
import com.QhomeBase.financebillingservice.repository.PricingTierRepository;
import com.QhomeBase.financebillingservice.repository.ServicePricingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PricingTierService {
    private final PricingTierRepository pricingTierRepository;
    private final ServicePricingRepository pricingRepository;

    @Transactional
    public PricingTierDto createPricingTier(CreatePricingTierRequest req, UUID createdBy) {
        String serviceCode = req.getServiceCode();
        
        if (req.getMaxQuantity() != null && req.getMinQuantity().compareTo(req.getMaxQuantity()) >= 0) {
            throw new IllegalArgumentException(
                "minQuantity must be < maxQuantity");
        }

        List<PricingTier> existingTiers = pricingTierRepository.findActiveTiersByService(serviceCode);
        
        if (!existingTiers.isEmpty()) {
            PricingTier lastPricingTier = existingTiers.get(existingTiers.size() - 1);
            BigDecimal lastMaxQuantity = lastPricingTier.getMaxQuantity();
            
            if (lastMaxQuantity != null) {
                BigDecimal expectedMin = lastMaxQuantity.add(BigDecimal.ONE);
                if (req.getMinQuantity().compareTo(expectedMin) != 0) {
                    throw new IllegalArgumentException(
                        String.format("Bậc tiếp theo phải bắt đầu từ %s (max của bậc trước + 1), không được là %s",
                            expectedMin, req.getMinQuantity()));
                }
            }
            
            if (req.getTierOrder() <= lastPricingTier.getTierOrder()) {
                throw new IllegalArgumentException(
                    String.format("Next tier order (%d) must be > previous tier order (%d)",
                        req.getTierOrder(), lastPricingTier.getTierOrder()));
            }
        } else {
            if (req.getMinQuantity().compareTo(BigDecimal.ONE) != 0 && req.getMinQuantity().compareTo(BigDecimal.ZERO) != 0) {
                throw new IllegalArgumentException(
                    String.format("Bậc đầu tiên phải bắt đầu từ 0 hoặc 1, không được là %s",
                        req.getMinQuantity()));
            }
        }
        
        checkForOverlaps(req.getMinQuantity(), req.getMaxQuantity(), req.getEffectiveFrom(), 
                        req.getEffectiveUntil(), existingTiers, null);

        OffsetDateTime now = OffsetDateTime.now();
        
        PricingTier newPricingTier = PricingTier.builder()
                .serviceCode(serviceCode)
                .tierOrder(req.getTierOrder())
                .minQuantity(req.getMinQuantity())
                .maxQuantity(req.getMaxQuantity())
                .unitPrice(req.getUnitPrice())
                .effectiveFrom(req.getEffectiveFrom())
                .effectiveUntil(req.getEffectiveUntil())
                .active(req.getActive() != null ? req.getActive() : true)
                .description(req.getDescription())
                .createdAt(now)
                .updatedAt(now)
                .createdBy(createdBy)
                .build();

        PricingTier saved = pricingTierRepository.save(newPricingTier);
        
        // Removed validateHasFinalTier - no longer require final tier
        
        log.info("Created pricing tier: id={}, serviceCode={}, tierOrder={}", 
                saved.getId(), saved.getServiceCode(), saved.getTierOrder());
        return toDto(saved);
    }
    private PricingTierDto toDto(PricingTier pricingTier) {
        return PricingTierDto.builder()
                .id(pricingTier.getId())
                .serviceCode(pricingTier.getServiceCode())
                .tierOrder(pricingTier.getTierOrder())
                .minQuantity(pricingTier.getMinQuantity())
                .maxQuantity(pricingTier.getMaxQuantity())
                .unitPrice(pricingTier.getUnitPrice())
                .effectiveFrom(pricingTier.getEffectiveFrom())
                .effectiveUntil(pricingTier.getEffectiveUntil())
                .active(pricingTier.getActive())
                .description(pricingTier.getDescription())
                .createdAt(pricingTier.getCreatedAt())
                .updatedAt(pricingTier.getUpdatedAt())
                .build();
    }
    @Transactional(readOnly = true)
    public List<PricingTierDto> getAllPricingTiers(String serviceCode) {
        List<PricingTier> tiers = pricingTierRepository.findActiveTiersByService(serviceCode);
        return tiers.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public PricingTierDto getById(UUID id) {
        PricingTier tier = pricingTierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pricing tier not found: " + id));
        return toDto(tier);
    }

    @Transactional
    public PricingTierDto updatePricingTier(UUID id, UpdatePricingTierRequest req, UUID updatedBy) {
        PricingTier tier = pricingTierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pricing tier not found: " + id));

        BigDecimal newMinQuantity = req.getMinQuantity() != null ? req.getMinQuantity() : tier.getMinQuantity();
        BigDecimal newMaxQuantity = req.getMaxQuantity() != null ? req.getMaxQuantity() : tier.getMaxQuantity();
        LocalDate newEffectiveFrom = req.getEffectiveFrom() != null ? req.getEffectiveFrom() : tier.getEffectiveFrom();
        LocalDate newEffectiveUntil = req.getEffectiveUntil() != null ? req.getEffectiveUntil() : tier.getEffectiveUntil();

        if (newMaxQuantity != null && newMinQuantity.compareTo(newMaxQuantity) >= 0) {
            throw new IllegalArgumentException(
                "minQuantity must be < maxQuantity");
        }

        List<PricingTier> existingTiers = pricingTierRepository.findActiveTiersByService(tier.getServiceCode());
        
        Integer currentTierOrder = req.getTierOrder() != null ? req.getTierOrder() : tier.getTierOrder();
        
        PricingTier previousTier = null;
        PricingTier nextTier = null;
        
        for (int i = 0; i < existingTiers.size(); i++) {
            PricingTier existingTier = existingTiers.get(i);
            if (existingTier.getId().equals(id)) {
                if (i > 0) {
                    previousTier = existingTiers.get(i - 1);
                }
                if (i < existingTiers.size() - 1) {
                    nextTier = existingTiers.get(i + 1);
                }
                break;
            }
        }
        
        if (previousTier == null && currentTierOrder > 1) {
            for (PricingTier existingTier : existingTiers) {
                if (existingTier.getId().equals(id)) continue;
                if (existingTier.getTierOrder() == currentTierOrder - 1) {
                    previousTier = existingTier;
                    break;
                }
            }
        }
        
        if (nextTier == null) {
            for (PricingTier existingTier : existingTiers) {
                if (existingTier.getId().equals(id)) continue;
                if (existingTier.getTierOrder() == currentTierOrder + 1) {
                    nextTier = existingTier;
                    break;
                }
            }
        }
        
        if (previousTier != null) {
            BigDecimal previousMax = previousTier.getMaxQuantity();
            if (previousMax != null) {
                BigDecimal expectedMin = previousMax.add(BigDecimal.ONE);
                if (newMinQuantity.compareTo(expectedMin) != 0) {
                    throw new IllegalArgumentException(
                        String.format("Bậc này phải bắt đầu từ %s (max của bậc trước + 1), không được là %s",
                            expectedMin, newMinQuantity));
                }
            }
        } else {
            if (newMinQuantity.compareTo(BigDecimal.ONE) != 0 && newMinQuantity.compareTo(BigDecimal.ZERO) != 0) {
                throw new IllegalArgumentException(
                    String.format("Bậc đầu tiên phải bắt đầu từ 0 hoặc 1, không được là %s",
                        newMinQuantity));
            }
        }
        
        if (nextTier != null) {
            BigDecimal nextMin = nextTier.getMinQuantity();
            if (newMaxQuantity != null) {
                BigDecimal expectedNextMin = newMaxQuantity.add(BigDecimal.ONE);
                if (nextMin.compareTo(expectedNextMin) != 0) {
                    throw new IllegalArgumentException(
                        String.format("Bậc tiếp theo phải bắt đầu từ %s (max của bậc này + 1), hiện tại là %s",
                            expectedNextMin, nextMin));
                }
            }
        }
        
        checkForOverlaps(newMinQuantity, newMaxQuantity, newEffectiveFrom, 
                        newEffectiveUntil, existingTiers, id);

        if (req.getTierOrder() != null) {
            tier.setTierOrder(req.getTierOrder());
        }
        if (req.getMinQuantity() != null) {
            tier.setMinQuantity(req.getMinQuantity());
        }
        if (req.getMaxQuantity() != null) {
            tier.setMaxQuantity(req.getMaxQuantity());
        }
        if (req.getUnitPrice() != null) {
            tier.setUnitPrice(req.getUnitPrice());
        }
        if (req.getEffectiveFrom() != null) {
            tier.setEffectiveFrom(req.getEffectiveFrom());
        }
        if (req.getEffectiveUntil() != null) {
            tier.setEffectiveUntil(req.getEffectiveUntil());
        }
        if (req.getActive() != null) {
            tier.setActive(req.getActive());
        }
        if (req.getDescription() != null) {
            tier.setDescription(req.getDescription());
        }

        tier.setUpdatedAt(OffsetDateTime.now());
        tier.setUpdatedBy(updatedBy);

        PricingTier updated = pricingTierRepository.save(tier);
        
        // Removed validateHasFinalTier - no longer require final tier
        
        log.info("Updated pricing tier: id={}, serviceCode={}", updated.getId(), updated.getServiceCode());
        return toDto(updated);
    }

    @Transactional
    public void deletePricingTier(UUID id) {
        PricingTier tier = pricingTierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pricing tier not found: " + id));
        
        // Allow deletion of final tier - removed restriction
        // Users can now delete final tiers if needed
        
        pricingTierRepository.deleteById(id);
        
        // Removed validateHasFinalTier - no longer require final tier
        
        log.info("Deleted pricing tier: id={}, serviceCode={}", id, tier.getServiceCode());
    }

    @Transactional(readOnly = true)
    public List<PricingTierDto> getActiveTiersByServiceAndDate(String serviceCode, LocalDate date) {
        List<PricingTier> tiers = pricingTierRepository.findActiveTiersByServiceAndDate(serviceCode, date);
        return tiers.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Integer getLastOrder(String serviceCode) {
        List<PricingTier> pricingTierList = pricingTierRepository.findActiveTiersByService(serviceCode);
        if (pricingTierList.isEmpty()) {
            return 0;
        }
        return pricingTierList.get(pricingTierList.size() - 1).getTierOrder();
    }
    private BigDecimal resolveUnitPrice(String serviceCode, LocalDate date) {
        return pricingRepository.findActivePriceGlobal(serviceCode, date)
                .map(sp -> sp.getBasePrice())
                .orElse(BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public List<CreateInvoiceLineRequest> calculateInvoiceLines(
            String serviceCode, 
            BigDecimal totalUsage, 
            LocalDate serviceDate, 
            String baseDescription) {
        
        List<PricingTier> tiers = pricingTierRepository.findActiveTiersByServiceAndDate(serviceCode, serviceDate);
        
        if (tiers.isEmpty()) {
            BigDecimal unitPrice = resolveUnitPrice(serviceCode, serviceDate);
            CreateInvoiceLineRequest line = CreateInvoiceLineRequest.builder()
                    .serviceDate(serviceDate)
                    .description(baseDescription)
                    .quantity(totalUsage)
                    .unit("kWh")
                    .unitPrice(unitPrice)
                    .taxRate(BigDecimal.ZERO)
                    .serviceCode(serviceCode)
                    .externalRefType("METER_READING_GROUP")
                    .externalRefId(null)
                    .build();
            return Collections.singletonList(line);
        }
        
        List<CreateInvoiceLineRequest> lines = new ArrayList<>();
        BigDecimal previousMax = BigDecimal.ZERO;
        PricingTier lastTier = null;
        
        for (PricingTier tier : tiers) {
            if (previousMax.compareTo(totalUsage) >= 0) {
                break;
            }
            
            BigDecimal tierEffectiveMax;
            if (tier.getMaxQuantity() == null) {
                tierEffectiveMax = totalUsage;
            } else {
                tierEffectiveMax = totalUsage.min(tier.getMaxQuantity());
            }
            
            BigDecimal applicableQuantity = tierEffectiveMax.subtract(previousMax).max(BigDecimal.ZERO);
            
            if (applicableQuantity.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal tierAmount = applicableQuantity.multiply(tier.getUnitPrice());
                
                String maxQtyStr = tier.getMaxQuantity() != null ? tier.getMaxQuantity().toString() : "∞";
                String tierDescription = String.format("%s (Bậc %d: %s-%s kWh)",
                        baseDescription,
                        tier.getTierOrder(),
                        tier.getMinQuantity(),
                        maxQtyStr);
                
                // Ensure unitPrice is from tier, not calculated amount
                BigDecimal unitPrice = tier.getUnitPrice();
                
                CreateInvoiceLineRequest line = CreateInvoiceLineRequest.builder()
                        .serviceDate(serviceDate)
                        .description(tierDescription)
                        .quantity(applicableQuantity)
                        .unit("kWh")
                        .unitPrice(unitPrice) // Use tier.getUnitPrice() directly, not tierAmount
                        .taxRate(BigDecimal.ZERO)
                        .serviceCode(serviceCode)
                        .externalRefType("METER_READING_GROUP")
                        .externalRefId(null)
                        .build();
                
                lines.add(line);
                previousMax = tierEffectiveMax;
                lastTier = tier;
                
                log.info("✅ [PricingTierService] Created invoice line - Tier {}: quantity={} kWh, unitPrice={} VND/kWh, lineTotal={} VND", 
                        tier.getTierOrder(), applicableQuantity, unitPrice, tierAmount);
            }
        }
        
        if (previousMax.compareTo(totalUsage) < 0 && lastTier != null && lastTier.getMaxQuantity() != null) {
            BigDecimal remainingQuantity = totalUsage.subtract(previousMax);
            
            String tierDescription = String.format("%s (Bậc %d: >%s kWh, dùng giá bậc cuối)",
                    baseDescription,
                    lastTier.getTierOrder(),
                    lastTier.getMaxQuantity());
            
            CreateInvoiceLineRequest line = CreateInvoiceLineRequest.builder()
                    .serviceDate(serviceDate)
                    .description(tierDescription)
                    .quantity(remainingQuantity)
                    .unit("kWh")
                    .unitPrice(lastTier.getUnitPrice())
                    .taxRate(BigDecimal.ZERO)
                    .serviceCode(serviceCode)
                    .externalRefType("METER_READING_GROUP")
                    .externalRefId(null)
                    .build();
            
            lines.add(line);
            
            log.warn("Usage {} kWh exceeds max tier quantity {} kWh. Using last tier price {} VND/kWh for remaining {} kWh",
                    totalUsage, lastTier.getMaxQuantity(), lastTier.getUnitPrice(), remainingQuantity);
        }
        
        if (lines.isEmpty()) {
            log.warn("No tiers matched for usage {} kWh, using simple pricing", totalUsage);
            BigDecimal unitPrice = resolveUnitPrice(serviceCode, serviceDate);
            CreateInvoiceLineRequest line = CreateInvoiceLineRequest.builder()
                    .serviceDate(serviceDate)
                    .description(baseDescription)
                    .quantity(totalUsage)
                    .unit("kWh")
                    .unitPrice(unitPrice)
                    .taxRate(BigDecimal.ZERO)
                    .serviceCode(serviceCode)
                    .externalRefType("METER_READING_GROUP")
                    .externalRefId(null)
                    .build();
            return Collections.singletonList(line);
        }
        
        return lines;
    }

    private void checkForOverlaps(BigDecimal minQuantity, BigDecimal maxQuantity, 
                                   LocalDate effectiveFrom, LocalDate effectiveUntil,
                                   List<PricingTier> existingTiers, UUID excludeId) {
        for (PricingTier existingTier : existingTiers) {
            if (excludeId != null && existingTier.getId().equals(excludeId)) {
                continue;
            }

            if (!isTierCurrentlyActive(existingTier, effectiveFrom, effectiveUntil)) {
                continue;
            }

            BigDecimal existingMin = existingTier.getMinQuantity();
            BigDecimal existingMax = existingTier.getMaxQuantity();

            if (hasOverlap(minQuantity, maxQuantity, existingMin, existingMax)) {
                String overlapRange = formatOverlapRange(
                    minQuantity, maxQuantity, existingMin, existingMax);
                throw new IllegalArgumentException(
                    String.format("Khoảng giá bị trùng với Bậc %d (%s). Vui lòng điều chỉnh min/max để tránh trùng lặp.",
                        existingTier.getTierOrder(), overlapRange));
            }
        }
    }

    private boolean isTierCurrentlyActive(PricingTier tier, LocalDate checkFrom, LocalDate checkUntil) {
        if (!Boolean.TRUE.equals(tier.getActive())) {
            return false;
        }

        LocalDate tierFrom = tier.getEffectiveFrom();
        LocalDate tierUntil = tier.getEffectiveUntil();

        if (tierFrom == null || checkFrom == null) {
            return false;
        }

        LocalDate tierUntilEffective = tierUntil != null ? tierUntil : LocalDate.MAX;
        LocalDate checkUntilEffective = checkUntil != null ? checkUntil : LocalDate.MAX;

        return !tierFrom.isAfter(checkUntilEffective) && !checkFrom.isAfter(tierUntilEffective);
    }

    private boolean hasOverlap(BigDecimal min1, BigDecimal max1, BigDecimal min2, BigDecimal max2) {
        if (max1 == null && max2 == null) {
            return true;
        }

        if (max1 == null) {
            return min1.compareTo(max2) < 0;
        }

        if (max2 == null) {
            return min2.compareTo(max1) < 0;
        }

        return min1.compareTo(max2) < 0 && min2.compareTo(max1) < 0;
    }

    private String formatOverlapRange(BigDecimal min1, BigDecimal max1, BigDecimal min2, BigDecimal max2) {
        BigDecimal overlapFrom = min1.max(min2);
        BigDecimal overlapTo;
        
        if (max1 == null && max2 == null) {
            return String.format("từ %s trở đi", overlapFrom);
        } else if (max1 == null) {
            overlapTo = max2;
        } else if (max2 == null) {
            overlapTo = max1;
        } else {
            overlapTo = max1.min(max2);
        }

        if (overlapFrom.compareTo(overlapTo) == 0) {
            return String.format("tại %s", overlapFrom);
        } else {
            return String.format("từ %s đến %s", overlapFrom, overlapTo);
        }
    }

}
