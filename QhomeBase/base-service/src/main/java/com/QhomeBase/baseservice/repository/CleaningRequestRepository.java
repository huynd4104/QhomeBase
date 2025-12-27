package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.CleaningRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface CleaningRequestRepository extends JpaRepository<CleaningRequest, UUID> {
    List<CleaningRequest> findByResidentIdOrderByCreatedAtDesc(UUID residentId);
    List<CleaningRequest> findByStatusOrderByCreatedAtAsc(String status);
    boolean existsByResidentIdAndStatusIgnoreCase(UUID residentId, String status);

    long countByResidentId(UUID residentId);

    long countByResidentIdAndStatusIgnoreCase(UUID residentId, String status);

    @Query(value = "select * from data.cleaning_requests " +
            "where resident_id = :residentId " +
            "order by created_at desc " +
            "limit :limit offset :offset", nativeQuery = true)
    List<CleaningRequest> findByResidentIdWithPagination(
            @Param("residentId") UUID residentId,
            @Param("limit") int limit,
            @Param("offset") int offset);

    @Query("select c from CleaningRequest c " +
            "where c.status = :status " +
            "and c.resendAlertSent = false " +
            "and coalesce(c.lastResentAt, c.createdAt) <= :deadline " +
            "and c.cleaningDate = :today")
    List<CleaningRequest> findPendingRequestsForReminder(
            @Param("status") String status,
            @Param("deadline") OffsetDateTime deadline,
            @Param("today") LocalDate today);

    @Query("select c from CleaningRequest c " +
            "where c.status = :status " +
            "and c.lastResentAt is not null " +
            "and c.lastResentAt <= :resendDeadline " +
            "and c.cleaningDate = :today")
    List<CleaningRequest> findResentRequestsForAutoCancel(
            @Param("status") String status,
            @Param("resendDeadline") OffsetDateTime resendDeadline,
            @Param("today") LocalDate today);

    @Query("select c from CleaningRequest c " +
            "where c.status = :status " +
            "and c.lastResentAt is null " +
            "and c.resendAlertSent = true " +
            "and c.createdAt <= :noResendDeadline " +
            "and c.cleaningDate = :today")
    List<CleaningRequest> findNonResentRequestsForAutoCancel(
            @Param("status") String status,
            @Param("noResendDeadline") OffsetDateTime noResendDeadline,
            @Param("today") LocalDate today);

    @Query("select c from CleaningRequest c " +
            "where c.residentId = :residentId " +
            "and c.status = 'DONE' " +
            "order by c.updatedAt desc")
    List<CleaningRequest> findByResidentIdAndStatusDone(@Param("residentId") UUID residentId);
}

