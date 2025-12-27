package com.QhomeBase.servicescardservice.repository;

import com.QhomeBase.servicescardservice.model.RegisterServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RegisterServiceRequestRepository extends JpaRepository<RegisterServiceRequest, UUID> {

    Optional<RegisterServiceRequest> findByIdAndUserId(UUID id, UUID userId);

    @Query("SELECT r FROM RegisterServiceRequest r LEFT JOIN FETCH r.images WHERE r.id = :id AND r.userId = :userId")
    Optional<RegisterServiceRequest> findByIdAndUserIdWithImages(@Param("id") UUID id, @Param("userId") UUID userId);

    List<RegisterServiceRequest> findByUserId(UUID userId);

    List<RegisterServiceRequest> findByUserIdAndUnitId(UUID userId, UUID unitId);
    
    List<RegisterServiceRequest> findByUnitId(UUID unitId);

    @Query("""
            SELECT DISTINCT r
            FROM RegisterServiceRequest r
            LEFT JOIN FETCH r.images
            WHERE r.serviceType = :serviceType
            ORDER BY r.createdAt DESC
            """)
    List<RegisterServiceRequest> findAllByServiceTypeWithImages(@Param("serviceType") String serviceType);

    @Query("""
            SELECT r
            FROM RegisterServiceRequest r
            LEFT JOIN FETCH r.images
            WHERE r.id = :id
            """)
    Optional<RegisterServiceRequest> findByIdWithImages(@Param("id") UUID id);

    Optional<RegisterServiceRequest> findByVnpayTransactionRef(String vnpayTransactionRef);

    List<RegisterServiceRequest> findAllByOrderByCreatedAtDesc();

    List<RegisterServiceRequest> findByPaymentStatusAndUpdatedAtBefore(String paymentStatus, OffsetDateTime updatedAtBefore);

    @Query(value = """
        SELECT r.* FROM card.register_vehicle r
        LEFT JOIN data.units u ON u.id = r.unit_id
        LEFT JOIN data.buildings b ON b.id = u.building_id
        WHERE r.service_type = 'VEHICLE_REGISTRATION'
          AND (:buildingId IS NULL OR b.id = :buildingId)
          AND (:unitId IS NULL OR r.unit_id = :unitId)
          AND (:status IS NULL OR r.status = :status)
        ORDER BY r.approved_at DESC NULLS LAST, r.created_at DESC
        """, nativeQuery = true)
    List<RegisterServiceRequest> findApprovedVehicleCardsByBuildingAndUnit(
        @Param("buildingId") UUID buildingId,
        @Param("unitId") UUID unitId,
        @Param("status") String status
    );
    List<RegisterServiceRequest> findByStatusAndUpdatedAtBefore(String status, OffsetDateTime updatedAtBefore);

    @Query("""
            SELECT r FROM RegisterServiceRequest r
            WHERE r.serviceType = :serviceType
              AND UPPER(r.paymentStatus) = 'PAID'
              AND UPPER(r.status) NOT IN ('REJECTED', 'CANCELLED')
            """)
    List<RegisterServiceRequest> findActivePaidCards(@Param("serviceType") String serviceType);

    List<RegisterServiceRequest> findByStatusAndPaymentStatus(String status, String paymentStatus);

    @Query("""
            SELECT r FROM RegisterServiceRequest r
            WHERE r.serviceType = :serviceType
              AND UPPER(TRIM(r.licensePlate)) = UPPER(TRIM(:licensePlate))
              AND UPPER(r.status) NOT IN ('REJECTED', 'CANCELLED')
              AND (UPPER(r.status) = 'APPROVED' OR UPPER(r.paymentStatus) = 'PAID')
            """)
    List<RegisterServiceRequest> findByServiceTypeAndLicensePlateIgnoreCase(
            @Param("serviceType") String serviceType,
            @Param("licensePlate") String licensePlate
    );


    @Query("""
            SELECT r FROM RegisterServiceRequest r
            WHERE r.serviceType = :serviceType
              AND UPPER(TRIM(r.licensePlate)) = UPPER(TRIM(:licensePlate))
              AND r.id != :excludeId
              AND UPPER(r.status) NOT IN ('REJECTED', 'CANCELLED')
              AND (UPPER(r.status) = 'APPROVED' OR UPPER(r.paymentStatus) = 'PAID')
            """)
    List<RegisterServiceRequest> findByServiceTypeAndLicensePlateIgnoreCaseExcludingId(
            @Param("serviceType") String serviceType,
            @Param("licensePlate") String licensePlate,
            @Param("excludeId") UUID excludeId
    );


    @Query("""
            SELECT r FROM RegisterServiceRequest r
            WHERE UPPER(r.paymentStatus) = 'PAYMENT_IN_PROGRESS'
              AND r.vnpayInitiatedAt IS NOT NULL
              AND r.vnpayInitiatedAt < :threshold
            """)
    List<RegisterServiceRequest> findExpiredVnpayPayments(@Param("threshold") OffsetDateTime threshold);

    @Query("""
            SELECT COUNT(r) > 0 FROM RegisterServiceRequest r
            WHERE r.reissuedFromCardId = :originalCardId
              AND UPPER(r.status) NOT IN ('REJECTED', 'CANCELLED')
            """)
    boolean existsReissuedCard(@Param("originalCardId") UUID originalCardId);


    @Query("""
            SELECT r FROM RegisterServiceRequest r
            LEFT JOIN FETCH r.images
            WHERE r.reissuedFromCardId = :originalCardId
              AND UPPER(r.status) NOT IN ('REJECTED', 'CANCELLED')
            ORDER BY r.createdAt DESC
            """)
    List<RegisterServiceRequest> findReissuedCards(@Param("originalCardId") UUID originalCardId);
}
