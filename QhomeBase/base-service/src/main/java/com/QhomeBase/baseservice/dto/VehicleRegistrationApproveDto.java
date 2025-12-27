package com.QhomeBase.baseservice.dto;

import jakarta.validation.constraints.Size;

public record VehicleRegistrationApproveDto(
        @Size(max = 500, message = "Note must not exceed 500 characters")
        String note
) {}


