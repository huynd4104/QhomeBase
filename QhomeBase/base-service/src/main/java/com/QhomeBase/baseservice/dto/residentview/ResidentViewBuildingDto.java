package com.QhomeBase.baseservice.dto.residentview;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResidentViewBuildingDto {
    private UUID buildingId;
    private String buildingCode;
    private String buildingName;
    private Long totalResidents;
    private Long occupiedUnits;
}
