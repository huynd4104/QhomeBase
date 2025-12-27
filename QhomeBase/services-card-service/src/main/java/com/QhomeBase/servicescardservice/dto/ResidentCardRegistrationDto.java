package com.QhomeBase.servicescardservice.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ResidentCardRegistrationDto(
        UUID id,
        UUID userId,
        UUID unitId,
        String requestType,
        UUID residentId,
        String fullName,
        String apartmentNumber,
        String buildingName,
        String citizenId,
        String phoneNumber,
        String note,
        String status,
        String paymentStatus,
        BigDecimal paymentAmount,
        OffsetDateTime paymentDate,
        String paymentGateway,
        String vnpayTransactionRef,
        String adminNote,
        UUID approvedBy,
        OffsetDateTime approvedAt,
        String rejectionReason,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}

