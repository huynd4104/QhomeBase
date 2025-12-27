package com.QhomeBase.financebillingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleRegistrationPaymentRequest {
    private UUID registrationId;
    private UUID userId;
    private UUID unitId;
    private String vehicleType;
    private String licensePlate;
    private String requestType;
    private String note;
    private BigDecimal amount;
    private OffsetDateTime paymentDate;
    private String transactionRef;
    private String transactionNo;
    private String bankCode;
    private String cardType;
    private String responseCode;
}


