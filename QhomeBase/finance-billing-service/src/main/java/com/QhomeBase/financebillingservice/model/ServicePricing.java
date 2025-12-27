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
@Table(name = "service_pricing", schema = "billing")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServicePricing {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "service_code", nullable = false)
    private String serviceCode;
    
    @Column(name = "service_name", nullable = false)
    private String serviceName;
    
    @Column(name = "category")
    private String category;
    
    @Column(name = "base_price", nullable = false, precision = 14, scale = 4)
    private BigDecimal basePrice;
    
    @Column(name = "unit", nullable = false)
    private String unit;
    
    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRate;
    
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
}




