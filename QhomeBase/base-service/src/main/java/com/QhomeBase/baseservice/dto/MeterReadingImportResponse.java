package com.QhomeBase.baseservice.dto;

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
    private List<UUID> invoiceIds;
    private String message;
}
