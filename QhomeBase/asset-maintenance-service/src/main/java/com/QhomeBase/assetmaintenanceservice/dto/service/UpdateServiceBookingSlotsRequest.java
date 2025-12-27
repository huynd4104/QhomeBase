package com.QhomeBase.assetmaintenanceservice.dto.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateServiceBookingSlotsRequest {

    @Valid
    @NotNull(message = "Slot is required")
    private ServiceBookingSlotRequest slot;
}

