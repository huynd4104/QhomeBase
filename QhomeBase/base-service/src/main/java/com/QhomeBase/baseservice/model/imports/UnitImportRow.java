package com.QhomeBase.baseservice.model.imports;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class UnitImportRow {
    private int rowNumber;
    private String buildingCode;
    private String buildingId;
    private String code;
    private Integer floor;
    private BigDecimal areaM2;
    private Integer bedrooms;
}


