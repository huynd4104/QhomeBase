package com.QhomeBase.servicescardservice.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record RegisterServiceRequestUpdateDto(
        String serviceType,
        String requestType,
        String note,
        String status,
        String vehicleType,
        String licensePlate,
        String vehicleBrand,
        String vehicleColor,
        String paymentStatus,
        BigDecimal paymentAmount,
        OffsetDateTime paymentDate,
        String paymentGateway,
        String vnpayTransactionRef
) {}

