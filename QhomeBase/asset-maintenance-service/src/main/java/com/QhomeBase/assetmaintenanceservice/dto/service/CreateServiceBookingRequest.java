package com.QhomeBase.assetmaintenanceservice.dto.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateServiceBookingRequest {

    @NotNull(message = "Service ID is required")
    private UUID serviceId;

    @NotNull(message = "Booking date is required")
    private LocalDate bookingDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private BigDecimal durationHours;

    private Integer numberOfPeople;

    @Size(max = 2000, message = "Purpose must not exceed 2000 characters")
    private String purpose;

    @NotNull(message = "Total amount is required")
    private BigDecimal totalAmount;

    private Boolean termsAccepted;

    @Valid
    private List<CreateServiceBookingItemRequest> items;

    @Valid
    private ServiceBookingSlotRequest slot;

    @AssertTrue(message = "End time must be after start time")
    public boolean isValidRange() {
        if (startTime == null || endTime == null) {
            return true;
        }
        return endTime.isAfter(startTime);
    }
}

