package com.QhomeBase.servicescardservice.repository;

import com.QhomeBase.servicescardservice.model.ElevatorCardRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ElevatorCardRegistrationRepository extends JpaRepository<ElevatorCardRegistration, UUID> {
    Optional<ElevatorCardRegistration> findByIdAndUserId(UUID id, UUID userId);

    Optional<ElevatorCardRegistration> findByVnpayTransactionRef(String vnpayTransactionRef);
    
    List<ElevatorCardRegistration> findAllByVnpayTransactionRef(String vnpayTransactionRef);

    List<ElevatorCardRegistration> findByResidentId(UUID residentId);

    List<ElevatorCardRegistration> findByResidentIdAndUnitId(UUID residentId, UUID unitId);

    List<ElevatorCardRegistration> findByUnitId(UUID unitId);

    List<ElevatorCardRegistration> findByUserId(UUID userId);

    List<ElevatorCardRegistration> findByUserIdAndUnitId(UUID userId, UUID unitId);

    List<ElevatorCardRegistration> findAllByOrderByCreatedAtDesc();

    List<ElevatorCardRegistration> findByPaymentStatusAndUpdatedAtBefore(String paymentStatus, OffsetDateTime updatedAtBefore);

    @Query(value = """
        SELECT e.* FROM card.elevator_card_registration e
        LEFT JOIN data.units u ON u.id = e.unit_id
        LEFT JOIN data.buildings b ON b.id = u.building_id
        WHERE (:buildingId IS NULL OR b.id = :buildingId)
          AND (:unitId IS NULL OR e.unit_id = :unitId)
          AND (:status IS NULL OR e.status = :status)
        ORDER BY e.approved_at DESC NULLS LAST, e.created_at DESC
        """, nativeQuery = true)
    List<ElevatorCardRegistration> findApprovedCardsByBuildingAndUnit(
        @Param("buildingId") UUID buildingId,
        @Param("unitId") UUID unitId,
        @Param("status") String status
    );
    List<ElevatorCardRegistration> findByStatusAndUpdatedAtBefore(String status, OffsetDateTime updatedAtBefore);

    /**
     * Đếm số thẻ thang máy đã đăng ký cho unit (đã thanh toán thành công)
     * Chỉ đếm các registration đã được thanh toán (PAID) hoặc đã được approve (APPROVED)
     * Không đếm các registration chưa thanh toán (UNPAID, PAYMENT_PENDING) hoặc bị reject (REJECTED)
     * Dùng cho hiển thị số thẻ đã thanh toán thành công
     */
    @Query("SELECT COUNT(e) FROM ElevatorCardRegistration e " +
           "WHERE e.unitId = :unitId " +
           "AND e.status NOT IN ('REJECTED', 'CANCELLED') " +
           "AND ( " +
           "       e.status IN ('APPROVED','ACTIVE','COMPLETED','ISSUED') " +
           "    OR (e.status IN ('PENDING','REVIEW_PENDING','PROCESSING','IN_PROGRESS','READY_FOR_PAYMENT','PAYMENT_PENDING') " +
           "        AND e.paymentStatus = 'PAID') " +
           "    OR (e.paymentStatus = 'PAID' AND e.status IS NULL) " +
           "    )")
    long countElevatorCardsByUnitId(@Param("unitId") UUID unitId);

    /**
     * Đếm số thẻ thang máy đã đăng ký cho unit (bao gồm cả chưa thanh toán)
     * Đếm TẤT CẢ các registration trừ REJECTED và CANCELLED
     * Dùng cho validation khi đăng ký thẻ mới để đảm bảo không vượt quá giới hạn
     */
    @Query("SELECT COUNT(e) FROM ElevatorCardRegistration e " +
           "WHERE e.unitId = :unitId " +
           "AND e.status NOT IN :excludedStatuses")
    long countAllElevatorCardsByUnitId(@Param("unitId") UUID unitId, 
                                       @Param("excludedStatuses") List<String> excludedStatuses);

    @Query("""
            SELECT e FROM ElevatorCardRegistration e
            WHERE UPPER(e.paymentStatus) = 'PAID'
              AND UPPER(e.status) NOT IN ('REJECTED', 'CANCELLED')
            """)
    List<ElevatorCardRegistration> findActivePaidCards();

    /**
     * Tìm các thẻ đã duyệt và đã thanh toán để cập nhật trạng thái (NEEDS_RENEWAL, SUSPENDED)
     */
    List<ElevatorCardRegistration> findByStatusAndPaymentStatus(String status, String paymentStatus);

    /**
     * Tìm các thẻ có VNPay payment đang pending quá thời gian timeout
     * Tìm các registration có:
     * - payment_status = 'PAYMENT_IN_PROGRESS'
     * - payment_gateway = 'VNPAY' hoặc vnpay_transaction_ref IS NOT NULL
     * - vnpay_initiated_at < threshold (quá 10 phút)
     */
    @Query("SELECT e FROM ElevatorCardRegistration e " +
           "WHERE e.paymentStatus = 'PAYMENT_IN_PROGRESS' " +
           "AND (e.paymentGateway = 'VNPAY' OR e.vnpayTransactionRef IS NOT NULL) " +
           "AND e.vnpayInitiatedAt IS NOT NULL " +
           "AND e.vnpayInitiatedAt < :threshold")
    List<ElevatorCardRegistration> findExpiredVnpayPayments(@Param("threshold") OffsetDateTime threshold);
}
