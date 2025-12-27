package com.QhomeBase.assetmaintenanceservice.dto.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceBookingSlotDto {

    private UUID id;
    private UUID bookingId;
    private UUID serviceId;
    private LocalDate slotDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private OffsetDateTime createdAt;
}

