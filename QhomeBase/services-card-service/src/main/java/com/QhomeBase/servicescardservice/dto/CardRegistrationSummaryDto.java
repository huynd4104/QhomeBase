package com.QhomeBase.servicescardservice.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CardRegistrationSummaryDto(
        UUID id,
        String cardType,
        UUID userId,
        UUID residentId,
        UUID unitId,
        String requestType,
        String status,
        String paymentStatus,
        BigDecimal paymentAmount,
        OffsetDateTime paymentDate,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String displayName,
        String reference,
        String apartmentNumber,
        String buildingName,
        String note,
        OffsetDateTime approvedAt,
        OffsetDateTime vnpayInitiatedAt,
        Boolean canReissue
) {
}


