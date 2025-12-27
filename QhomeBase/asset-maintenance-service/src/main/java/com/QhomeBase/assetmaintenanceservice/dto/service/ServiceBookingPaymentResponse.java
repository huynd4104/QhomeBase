package com.QhomeBase.assetmaintenanceservice.dto.service;

import java.util.UUID;

public record ServiceBookingPaymentResponse(
        UUID bookingId,
        String paymentUrl,
        String transactionRef
) {
}

