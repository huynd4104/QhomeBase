package com.QhomeBase.financebillingservice.repository;

import com.QhomeBase.financebillingservice.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByPayerResidentId(UUID residentId);
}




