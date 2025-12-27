package com.QhomeBase.baseservice.dto;

import com.QhomeBase.baseservice.model.HouseholdMemberRequest.RequestStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record HouseholdMemberRequestDto(
        UUID id,
        UUID householdId,
        String householdCode,
        UUID unitId,
        String unitCode,
        UUID residentId,
        String residentName,
        String residentEmail,
        String residentPhone,
        String requestedResidentFullName,
        String requestedResidentPhone,
        String requestedResidentEmail,
        String requestedResidentNationalId,
        java.time.LocalDate requestedResidentDob,
        UUID requestedBy,
        String requestedByName,
        String relation,
        String proofOfRelationImageUrl,
        String note,
        RequestStatus status,
        UUID approvedBy,
        String approvedByName,
        UUID rejectedBy,
        String rejectedByName,
        String rejectionReason,
        OffsetDateTime approvedAt,
        OffsetDateTime rejectedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
