package com.QhomeBase.assetmaintenanceservice.dto.service;

import java.util.UUID;

public record ServiceBookingPaymentResult(
        UUID bookingId,
        boolean success,
        String responseCode,
        boolean signatureValid
) {
}

