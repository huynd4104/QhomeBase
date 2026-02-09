package com.QhomeBase.financebillingservice.repository;

import com.QhomeBase.financebillingservice.model.ServicePricing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServicePricingRepository extends JpaRepository<ServicePricing, UUID> {
    
    @Query("""
        SELECT sp FROM ServicePricing sp
        WHERE sp.serviceCode = :serviceCode
          AND sp.active = true
          AND :effectiveDate BETWEEN sp.effectiveFrom 
              AND COALESCE(sp.effectiveUntil, '9999-12-31')
        ORDER BY sp.effectiveFrom DESC
        """)
    Optional<ServicePricing> findActivePriceGlobal(
            @Param("serviceCode") String serviceCode,
            @Param("effectiveDate") LocalDate effectiveDate
    );
}




