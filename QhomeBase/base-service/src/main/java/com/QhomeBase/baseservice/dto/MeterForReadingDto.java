package com.QhomeBase.baseservice.dto;

import java.time.LocalDate;
import java.util.UUID;

public record MeterForReadingDto(
        UUID meterId,
        UUID unitId,
        String unitCode,
        Integer floor,
        String meterCode,
        LocalDate installedAt,
        Boolean hasReading
) {}

