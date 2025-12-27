package com.QhomeBase.financebillingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeterReadingImportResponse {
    private int totalReadings;
    private int invoicesCreated;
    private int invoicesSkipped;
    private List<UUID> invoiceIds;
    private List<String> errors;
    private String message;
}

