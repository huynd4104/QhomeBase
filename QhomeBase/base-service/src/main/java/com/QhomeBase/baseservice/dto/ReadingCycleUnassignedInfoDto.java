package com.QhomeBase.baseservice.dto;

import java.util.List;
import java.util.UUID;

public record ReadingCycleUnassignedInfoDto(
        UUID cycleId,
        UUID serviceId,
        int totalUnassigned,
        List<ReadingCycleUnassignedFloorDto> floors,
        String message,
        List<UnitWithoutMeterDto> missingMeterUnits
) {
    public record ReadingCycleUnassignedFloorDto(
            UUID buildingId,
            String buildingCode,
            String buildingName,
            Integer floor,
            List<String> unitCodes
    ) {}
}


