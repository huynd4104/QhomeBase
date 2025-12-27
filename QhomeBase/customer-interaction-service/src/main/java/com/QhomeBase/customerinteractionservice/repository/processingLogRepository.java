package com.QhomeBase.customerinteractionservice.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.QhomeBase.customerinteractionservice.model.ProcessingLog;

@Repository
public interface processingLogRepository extends JpaRepository<ProcessingLog, UUID> {

   List<ProcessingLog> findByRecordIdOrderByCreatedAtDesc(UUID recordId);

   List<ProcessingLog> findByStaffInCharge(UUID staffId);

}
