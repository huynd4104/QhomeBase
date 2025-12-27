package com.QhomeBase.customerinteractionservice.client.dto;

import java.time.LocalDate;
import java.util.UUID;

public record HouseholdMemberDto(
        UUID id,
        UUID householdId,
        UUID residentId,
        Boolean isPrimary,
        LocalDate joinedAt,
        LocalDate leftAt
) {
}




