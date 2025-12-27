package com.QhomeBase.financebillingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ElectricityMonthlyDto {
    private String month; // "YYYY-MM"
    private String monthDisplay; // "MM/yyyy"
    private BigDecimal amount;
    private Integer year;
    private Integer monthNumber; // 1-12
}

