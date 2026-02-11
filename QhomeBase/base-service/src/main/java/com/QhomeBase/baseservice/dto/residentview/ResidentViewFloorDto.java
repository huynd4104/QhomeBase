package com.QhomeBase.baseservice.dto.residentview;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResidentViewFloorDto {
    private Integer floor;
    private Long totalUnits;
    private Long occupiedUnits;
}
