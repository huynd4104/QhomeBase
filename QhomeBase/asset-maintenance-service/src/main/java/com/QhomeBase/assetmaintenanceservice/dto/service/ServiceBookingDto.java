package com.QhomeBase.assetmaintenanceservice.dto.service;

import com.QhomeBase.assetmaintenanceservice.model.service.enums.ServiceBookingStatus;
import com.QhomeBase.assetmaintenanceservice.model.service.enums.ServicePaymentStatus;
import com.QhomeBase.assetmaintenanceservice.model.service.enums.ServicePricingType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceBookingDto {

    private UUID id;
    private UUID serviceId;
    private String serviceCode;
    private String serviceName;
    private ServicePricingType servicePricingType;
    private LocalDate bookingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal durationHours;
    private Integer numberOfPeople;
    private String purpose;
    private BigDecimal totalAmount;
    private ServicePaymentStatus paymentStatus;
    private OffsetDateTime paymentDate;
    private String paymentGateway;
    private String vnpayTransactionRef;
    private ServiceBookingStatus status;
    private UUID userId;
    private UUID approvedBy;
    private OffsetDateTime approvedAt;
    private String rejectionReason;
    private Boolean termsAccepted;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<ServiceBookingItemDto> bookingItems;
    private List<ServiceBookingSlotDto> bookingSlots;
}

