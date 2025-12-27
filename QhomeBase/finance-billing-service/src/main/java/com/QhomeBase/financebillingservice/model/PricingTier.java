package com.QhomeBase.financebillingservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "pricing_tiers", schema = "billing")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingTier {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "service_code", nullable = false)
    private String serviceCode;
    
    @Column(name = "tier_order", nullable = false)
    private Integer tierOrder;
    
    @Column(name = "min_quantity", nullable = false, precision = 14, scale = 3)
    private BigDecimal minQuantity;
    
    @Column(name = "max_quantity", precision = 14, scale = 3)
    private BigDecimal maxQuantity;
    
    @Column(name = "unit_price", nullable = false, precision = 14, scale = 4)
    private BigDecimal unitPrice;
    
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;
    
    @Column(name = "effective_until")
    private LocalDate effectiveUntil;
    
    @Column(name = "active", nullable = false)
    private Boolean active;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    
    @Column(name = "created_by")
    private UUID createdBy;
    
    @Column(name = "updated_by")
    private UUID updatedBy;
    
    public boolean isInRange(BigDecimal quantity) {
        if (quantity.compareTo(minQuantity) < 0) {
            return false;
        }
        if (maxQuantity == null) {
            return true;
        }
        return quantity.compareTo(maxQuantity) <= 0;
    }
    
    public BigDecimal getApplicableQuantity(BigDecimal totalQuantity) {
        if (totalQuantity.compareTo(minQuantity) <= 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal tierEffectiveMax;
        if (maxQuantity == null) {
            tierEffectiveMax = totalQuantity;
        } else {
            tierEffectiveMax = totalQuantity.min(maxQuantity);
        }
        
        BigDecimal applicable = tierEffectiveMax.subtract(minQuantity);
        return applicable.max(BigDecimal.ZERO);
    }
}

