package com.QhomeBase.servicescardservice.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ElevatorCardRegistrationUpdateDto(
        String fullName,
        String apartmentNumber,
        String buildingName,
        String requestType,
        String citizenId,
        String phoneNumber,
        String note,
        String status,
        String paymentStatus,
        BigDecimal paymentAmount,
        OffsetDateTime paymentDate,
        String paymentGateway,
        String vnpayTransactionRef
) {}

