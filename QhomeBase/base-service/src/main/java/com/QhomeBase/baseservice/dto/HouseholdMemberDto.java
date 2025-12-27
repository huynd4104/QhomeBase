package com.QhomeBase.baseservice.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record HouseholdMemberDto(
        UUID id,
        UUID householdId,
        UUID residentId,
        String residentName,
        String residentEmail,
        String residentPhone,
        String relation,
        String proofOfRelationImageUrl,
        Boolean isPrimary,
        LocalDate joinedAt,
        LocalDate leftAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}

