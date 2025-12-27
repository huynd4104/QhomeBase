package com.QhomeBase.financebillingservice.repository;

import com.QhomeBase.financebillingservice.model.InvoiceLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InvoiceLineRepository extends JpaRepository<InvoiceLine, UUID> {
    
    List<InvoiceLine> findByInvoiceId(UUID invoiceId);
    
    List<InvoiceLine> findByExternalRefId(UUID externalRefId);
    
    List<InvoiceLine> findByServiceCode(String serviceCode);
    
    List<InvoiceLine> findByInvoiceIdAndServiceCode(UUID invoiceId, String serviceCode);
}








