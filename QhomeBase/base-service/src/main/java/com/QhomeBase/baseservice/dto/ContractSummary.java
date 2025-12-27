package com.QhomeBase.baseservice.dto;

import java.time.LocalDate;
import java.util.UUID;

public record ContractSummary(
        UUID id,
        UUID unitId,
        String contractNumber,
        String contractType,
        LocalDate startDate,
        LocalDate endDate,
        String status
) {
    public boolean isActiveOn(LocalDate date) {
        if (date == null) {
            return true;
        }
        LocalDate start = startDate;
        LocalDate end = endDate;

        boolean afterStart = start == null || !date.isBefore(start);
        boolean beforeEnd = end == null || !date.isAfter(end);
        boolean statusActive = status == null
                || status.equalsIgnoreCase("ACTIVE")
                || status.equalsIgnoreCase("PENDING");
        return afterStart && beforeEnd && statusActive;
    }
}

