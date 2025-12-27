package com.QhomeBase.servicescardservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

public record ResidentCardRegistrationCreateDto(
        @NotNull(message = "Unit ID is required")
        UUID unitId,

        String requestType,

        @NotNull(message = "Resident ID is required")
        UUID residentId,

        String fullName,

        String apartmentNumber,

        String buildingName,

        @NotBlank(message = "CCCD/CMND là bắt buộc")
        @Pattern(regexp = "^[0-9]{12,}$", message = "CCCD/CMND phải có ít nhất 12 số")
        String citizenId,

        String phoneNumber,

        String note
) {}

