package com.QhomeBase.baseservice.dto;

import java.time.LocalDate;
import java.util.UUID;

public record HouseholdMemberUpdateDto(
        UUID householdId,
        UUID residentId,
        String relation,
        String proofOfRelationImageUrl,
        Boolean isPrimary,
        LocalDate joinedAt,
        LocalDate leftAt
) {}

