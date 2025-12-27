package com.QhomeBase.baseservice.dto.imports;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeterImportRowResult {
    private int rowNumber;
    private boolean success;
    private String message;
}


