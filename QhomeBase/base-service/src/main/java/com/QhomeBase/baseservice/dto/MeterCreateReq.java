package com.QhomeBase.baseservice.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record MeterCreateReq(
        @NotNull UUID unitId,
        @NotNull UUID serviceId,
        String meterCode,
        LocalDate installedAt
) {}

