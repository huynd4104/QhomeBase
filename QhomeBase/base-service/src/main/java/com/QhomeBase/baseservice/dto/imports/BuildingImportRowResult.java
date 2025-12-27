package com.QhomeBase.baseservice.dto.imports;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildingImportRowResult {
    private int rowNumber;
    private boolean success;
    private String message;
    private String buildingId;
    private String code;
    private String name;
}








