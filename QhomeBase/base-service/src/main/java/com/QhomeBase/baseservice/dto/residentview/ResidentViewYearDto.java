package com.QhomeBase.baseservice.dto.residentview;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResidentViewYearDto {
    private Integer year;
    private Long totalResidents;
    private Long occupiedUnits;
}
