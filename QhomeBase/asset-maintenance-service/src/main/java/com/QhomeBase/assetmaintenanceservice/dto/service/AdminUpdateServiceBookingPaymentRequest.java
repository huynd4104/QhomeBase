package com.QhomeBase.assetmaintenanceservice.dto.service;

import com.QhomeBase.assetmaintenanceservice.model.service.enums.ServicePaymentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUpdateServiceBookingPaymentRequest {

    @NotNull(message = "Payment status is required")
    private ServicePaymentStatus paymentStatus;

    private OffsetDateTime paymentDate;

    @Size(max = 64, message = "Payment gateway must not exceed 64 characters")
    private String paymentGateway;

    @Size(max = 128, message = "Transaction reference must not exceed 128 characters")
    private String transactionReference;
}

