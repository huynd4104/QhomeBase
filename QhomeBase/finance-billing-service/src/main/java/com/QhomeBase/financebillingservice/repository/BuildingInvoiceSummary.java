package com.QhomeBase.financebillingservice.repository;

import java.math.BigDecimal;
import java.util.UUID;

public interface BuildingInvoiceSummary {
    UUID getBuildingId();
    String getBuildingCode();
    String getBuildingName();
    String getStatus();
    BigDecimal getTotalAmount();
    Long getInvoiceCount();
}

