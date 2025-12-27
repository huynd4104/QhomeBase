package com.QhomeBase.financebillingservice.repository;

import com.QhomeBase.financebillingservice.model.Invoice;
import com.QhomeBase.financebillingservice.model.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    
    List<Invoice> findByPayerUnitId(UUID unitId);
    
    List<Invoice> findByPayerResidentId(UUID residentId);
    
    List<Invoice> findByStatus(InvoiceStatus status);
    
    List<Invoice> findByPayerResidentIdAndStatus(UUID residentId, InvoiceStatus status);
    
    /**
     * Tìm invoice theo cả payerResidentId VÀ payerUnitId
     * Để tất cả thành viên trong cùng căn hộ có thể xem invoice của căn hộ đó
     */
    @Query("SELECT i FROM Invoice i WHERE i.payerResidentId = :residentId OR i.payerUnitId = :unitId")
    List<Invoice> findByPayerResidentIdOrPayerUnitId(@Param("residentId") UUID residentId, @Param("unitId") UUID unitId);
    
    /**
     * Tìm invoice theo cả payerResidentId VÀ payerUnitId với status
     */
    @Query("SELECT i FROM Invoice i WHERE (i.payerResidentId = :residentId OR i.payerUnitId = :unitId) AND i.status = :status")
    List<Invoice> findByPayerResidentIdOrPayerUnitIdAndStatus(@Param("residentId") UUID residentId, @Param("unitId") UUID unitId, @Param("status") InvoiceStatus status);
    
    @Query("SELECT i FROM Invoice i WHERE i.payerUnitId = :unitId AND i.cycleId = :cycleId")
    List<Invoice> findByPayerUnitIdAndCycleId(@Param("unitId") UUID unitId, @Param("cycleId") UUID cycleId);

    Optional<Invoice> findByVnpTransactionRef(String vnpTransactionRef);

    @Query(value = """
    select i.*
    from billing.invoices i
    where i.cycle_id =  :cycleId
""", nativeQuery = true)
    List<Invoice> findByCycleId( @Param("cycleId") UUID cycleId);

     @Query(value = """
     SELECT i.*
     FROM billing.invoices i
     INNER JOIN data.units u ON i.payer_unit_id = u.id
     WHERE u.building_id = :buildingId
       AND i.cycle_id = :cycleId
 """, nativeQuery = true)
     List<Invoice> findByCycleIdAndBuildingId(@Param("cycleId") UUID cycleId, @Param("buildingId") UUID buildingId);

    List<Invoice> findByCycleIdAndStatus(UUID cycleId, InvoiceStatus status);

    List<Invoice> findByCycleIdAndStatusIn(UUID cycleId, List<InvoiceStatus> statuses);

    List<Invoice> findByCycleIdAndPayerUnitId(UUID cycleId, UUID unitId);

    @Query(value = """
    SELECT u.building_id as buildingId,
           b.code as buildingCode,
           b.name as buildingName,
           i.status as status,
           COALESCE(SUM((il.quantity * il.unit_price) + il.tax_amount), 0) as totalAmount,
           COUNT(DISTINCT i.id) as invoiceCount
    FROM billing.invoices i
    LEFT JOIN billing.invoice_lines il ON il.invoice_id = i.id
    LEFT JOIN data.units u ON u.id = i.payer_unit_id
    LEFT JOIN data.buildings b ON b.id = u.building_id
    WHERE i.cycle_id = :cycleId
      AND u.building_id IS NOT NULL
    GROUP BY u.building_id, b.code, b.name, i.status
    """, nativeQuery = true)
    List<BuildingInvoiceSummary> summarizeByCycleAndBuilding(
            @Param("cycleId") UUID cycleId);


    @Query(value = """
    select i.*
    from billing.invoices i
    join billing.invoice_lines bi on bi.invoice_id = i.id
    join billing.billing_cycles bc on bc.id = i.cycle_id
    where bi.service_code = :serviceCode
    and i.cycle_id = :cycleID
""", nativeQuery = true)
    List<Invoice> findByServiceCodeAndAndCycle(@Param("cycleID") UUID cycleID, @Param("serviceCode") String serviceCode);

    /**
     * Tìm các invoice có VNPay payment đang pending quá thời gian timeout
     * - paymentGateway = 'VNPAY'
     * - status != 'PAID'
     * - vnpayInitiatedAt != null và đã quá threshold
     */
    @Query(value = """
        SELECT i.*
        FROM billing.invoices i
        WHERE i.payment_gateway = 'VNPAY'
          AND i.status != 'PAID'
          AND i.vnpay_initiated_at IS NOT NULL
          AND i.vnpay_initiated_at < :threshold
    """, nativeQuery = true)
    List<Invoice> findExpiredVnpayPayments(@Param("threshold") java.time.OffsetDateTime threshold);
}
