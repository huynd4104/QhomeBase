package com.QhomeBase.baseservice.dto;

import com.QhomeBase.baseservice.model.AccountCreationRequest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AccountCreationRequestDto(
        UUID id,
        UUID residentId,
        String residentName,
        String residentEmail,
        String residentPhone,
        UUID householdId,
        UUID unitId,
        String unitCode,
        String relation,
        UUID requestedBy,
        String requestedByName,
        String username,
        String email,
        boolean autoGenerate,
        AccountCreationRequest.RequestStatus status,
        UUID approvedBy,
        String approvedByName,
        UUID rejectedBy,
        String rejectedByName,
        String rejectionReason,
        List<String> proofOfRelationImageUrls,
        OffsetDateTime approvedAt,
        OffsetDateTime rejectedAt,
        OffsetDateTime createdAt
) {}

