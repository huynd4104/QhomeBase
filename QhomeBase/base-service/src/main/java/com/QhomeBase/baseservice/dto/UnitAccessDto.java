package com.QhomeBase.baseservice.dto;

import java.util.UUID;

public record UnitAccessDto(
        UUID unitId,
        String unitCode,
        boolean isPrimary,
        String relation
) {}


