package com.QhomeBase.servicescardservice.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record RegisterServiceRequestDto(
        UUID id,
        UUID userId,
        String serviceType,
        String requestType,
        String note,
        String status,
        String vehicleType,
        String licensePlate,
        String vehicleBrand,
        String vehicleColor,
        String apartmentNumber,
        String buildingName,
        UUID unitId,
        String paymentStatus,
        BigDecimal paymentAmount,
        OffsetDateTime paymentDate,
        String paymentGateway,
        String vnpayTransactionRef,
        String adminNote,
        UUID approvedBy,
        String approvedByName,
        OffsetDateTime approvedAt,
        String rejectionReason,
        List<RegisterServiceImageDto> images,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        UUID reissuedFromCardId,
        Boolean canReissue
) {}

