package com.QhomeBase.customerinteractionservice.client.dto;

import java.time.LocalDate;
import java.util.UUID;

public record HouseholdDto(
        UUID id,
        UUID unitId,
        LocalDate startDate,
        LocalDate endDate
) {
}




