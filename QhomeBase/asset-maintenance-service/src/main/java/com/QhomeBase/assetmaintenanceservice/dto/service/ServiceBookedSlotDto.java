package com.QhomeBase.assetmaintenanceservice.dto.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceBookedSlotDto {

    private UUID bookingId;
    private LocalDate slotDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String bookingStatus;
}
