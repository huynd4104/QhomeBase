package com.QhomeBase.baseservice.dto;

import java.time.LocalDate;

public record MeterUpdateReq(
        String meterCode,
        Boolean active,
        LocalDate removedAt
) {}

