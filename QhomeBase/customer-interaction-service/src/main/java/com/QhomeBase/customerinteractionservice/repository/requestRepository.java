package com.QhomeBase.customerinteractionservice.repository;

import java.util.List;
import java.util.UUID;

import com.QhomeBase.customerinteractionservice.dto.StatusCountDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.QhomeBase.customerinteractionservice.model.Request;

@Repository
public interface requestRepository extends JpaRepository<Request, UUID>, JpaSpecificationExecutor<Request> {

    @Query(value = "SELECT CAST(r.status AS TEXT) AS status, COUNT(r) AS count " +
            "FROM cs_service.requests r " +
            "WHERE (CAST(:dateFrom AS DATE) IS NULL OR r.created_at >= CAST(:dateFrom AS DATE)) " +
            "AND (CAST(:dateTo AS DATE) IS NULL OR r.created_at < (CAST(:dateTo AS DATE) + interval '1 day')) " +
            "GROUP BY r.status", nativeQuery = true)
    List<StatusCountDTO> countRequestsByStatus(
            @Param("dateFrom") String dateFrom,
            @Param("dateTo") String dateTo
    );

    @Query(value = "SELECT COUNT(r) FROM cs_service.requests r " +
            "WHERE r.resident_id = :residentId " +
            "AND r.created_at >= :since", nativeQuery = true)
    long countRequestsByResidentSince(
            @Param("residentId") UUID residentId,
            @Param("since") java.time.LocalDateTime since
    );

}
