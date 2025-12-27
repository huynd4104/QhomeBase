package com.QhomeBase.financebillingservice.repository;

import com.QhomeBase.financebillingservice.model.PricingTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface   PricingTierRepository extends JpaRepository<PricingTier, UUID> {
    
    @Query("""
        SELECT pt FROM PricingTier pt
        WHERE pt.serviceCode = :serviceCode
          AND pt.active = true
          AND :effectiveDate BETWEEN pt.effectiveFrom 
              AND COALESCE(pt.effectiveUntil, '9999-12-31')
        ORDER BY pt.tierOrder ASC
        """)
    List<PricingTier> findActiveTiersByServiceAndDate(
            @Param("serviceCode") String serviceCode,
            @Param("effectiveDate") LocalDate effectiveDate
    );
    @Query("""
        SELECT pt FROM PricingTier pt
        WHERE pt.serviceCode = :serviceCode
          AND pt.active = true
        ORDER BY pt.tierOrder ASC
        """)
    List<PricingTier> findActiveTiersByService(
            @Param("serviceCode") String serviceCode
    );

    
    @Query("""
        SELECT pt FROM PricingTier pt
        WHERE pt.serviceCode = :serviceCode
          AND pt.tierOrder = :tierOrder
          AND pt.active = true
          AND :effectiveDate BETWEEN pt.effectiveFrom 
              AND COALESCE(pt.effectiveUntil, '9999-12-31')
        ORDER BY pt.effectiveFrom DESC
        """)
    Optional<PricingTier> findByServiceCodeAndTierOrderAndDate(
            @Param("serviceCode") String serviceCode,
            @Param("tierOrder") Integer tierOrder,
            @Param("effectiveDate") LocalDate effectiveDate
    );
    
    List<PricingTier> findByServiceCodeOrderByTierOrderAsc(String serviceCode);
    
    List<PricingTier> findByServiceCodeAndActiveOrderByTierOrderAsc(String serviceCode, Boolean active);
}

