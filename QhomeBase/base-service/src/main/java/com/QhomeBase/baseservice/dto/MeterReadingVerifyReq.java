package com.QhomeBase.baseservice.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MeterReadingVerifyReq(
        @NotNull Boolean verified,
        @NotNull UUID verifiedBy,
        String note
) {}

