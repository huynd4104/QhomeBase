package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.MaintenanceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MaintenanceRequestRepository extends JpaRepository<MaintenanceRequest, UUID>, JpaSpecificationExecutor<MaintenanceRequest> {
    List<MaintenanceRequest> findByResidentIdOrderByCreatedAtDesc(UUID residentId);
    List<MaintenanceRequest> findByStatusOrderByCreatedAtAsc(String status);
    boolean existsByResidentIdAndStatusIgnoreCase(UUID residentId, String status);

    long countByResidentId(UUID residentId);

    long countByResidentIdAndStatusIgnoreCase(UUID residentId, String status);

    @Query(value = "select * from data.maintenance_requests " +
            "where resident_id = :residentId " +
            "order by created_at desc " +
            "limit :limit offset :offset", nativeQuery = true)
    List<MaintenanceRequest> findByResidentIdWithPagination(
            @Param("residentId") UUID residentId,
            @Param("limit") int limit,
            @Param("offset") int offset);

    @Query("select m from MaintenanceRequest m " +
            "where m.status = :status " +
            "and m.resendAlertSent = false " +
            "and m.callAlertSent = false " +
            "and coalesce(m.lastResentAt, m.createdAt) <= :deadline " +
            "and m.preferredDatetime is not null " +
            "and CAST(m.preferredDatetime AS date) = CAST(:today AS date)")
    List<MaintenanceRequest> findPendingRequestsForReminder(
            @Param("status") String status,
            @Param("deadline") OffsetDateTime deadline,
            @Param("today") OffsetDateTime today);

    @Query("select m from MaintenanceRequest m " +
            "where m.status = :status " +
            "and m.resendAlertSent = true " +
            "and m.callAlertSent = false " +
            "and coalesce(m.lastResentAt, m.createdAt) <= :deadline " +
            "and m.preferredDatetime is not null " +
            "and CAST(m.preferredDatetime AS date) = CAST(:today AS date)")
    List<MaintenanceRequest> findPendingRequestsForCallAlert(
            @Param("status") String status,
            @Param("deadline") OffsetDateTime deadline,
            @Param("today") OffsetDateTime today);

    java.util.Optional<MaintenanceRequest> findByVnpayTransactionRef(String vnpayTransactionRef);

    @Query("select m from MaintenanceRequest m " +
            "where m.residentId = :residentId " +
            "and m.paymentStatus = 'PAID' " +
            "order by m.paymentDate desc nulls last, m.createdAt desc")
    List<MaintenanceRequest> findByResidentIdAndPaymentStatusPaid(@Param("residentId") UUID residentId);

    /**
     * Check if resident has any active maintenance request (status not DONE or CANCELLED)
     */
    @Query("select count(m) > 0 from MaintenanceRequest m " +
            "where m.residentId = :residentId " +
            "and m.status not in ('DONE', 'CANCELLED')")
    boolean existsActiveRequestByResidentId(@Param("residentId") UUID residentId);
}

