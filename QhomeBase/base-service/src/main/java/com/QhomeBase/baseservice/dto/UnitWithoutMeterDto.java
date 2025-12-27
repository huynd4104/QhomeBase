package com.QhomeBase.baseservice.dto;

import java.util.UUID;

public record UnitWithoutMeterDto(
        UUID unitId,
        String unitCode,
        Integer floor,
        UUID buildingId,
        String buildingCode,
        String buildingName,
        UUID serviceId,
        String serviceCode,
        String serviceName
) {}


