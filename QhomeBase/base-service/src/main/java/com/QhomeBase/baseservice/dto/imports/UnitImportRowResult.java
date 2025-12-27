package com.QhomeBase.baseservice.dto.imports;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnitImportRowResult {
    private int rowNumber;
    private boolean success;
    private String message;
    private String unitId;
    private String buildingId;
    private String buildingCode;
    private String code;
}








