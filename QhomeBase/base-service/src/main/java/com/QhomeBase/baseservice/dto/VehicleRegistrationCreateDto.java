package com.QhomeBase.baseservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record VehicleRegistrationCreateDto(
        @NotNull(message = "Vehicle ID is required")
        UUID vehicleId,

        @Size(max = 500, message = "Reason must not exceed 500 characters")
        String reason
) {}


