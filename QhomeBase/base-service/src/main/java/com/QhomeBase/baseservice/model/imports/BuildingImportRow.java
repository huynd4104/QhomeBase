package com.QhomeBase.baseservice.model.imports;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BuildingImportRow {
    private int rowNumber;
    private String code;
    private String name;
    private String address;
}


