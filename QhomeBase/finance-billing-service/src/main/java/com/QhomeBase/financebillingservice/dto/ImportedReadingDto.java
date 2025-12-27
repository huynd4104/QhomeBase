package com.QhomeBase.financebillingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportedReadingDto {
    private UUID unitId;
    private UUID residentId;
    private UUID cycleId;
    private LocalDate readingDate;
    private BigDecimal usageKwh;
    private String serviceCode;
    private String description;
    private UUID externalReadingId;
}


