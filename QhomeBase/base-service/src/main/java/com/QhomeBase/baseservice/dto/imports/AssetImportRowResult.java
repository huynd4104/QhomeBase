package com.QhomeBase.baseservice.dto.imports;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetImportRowResult {
    private int rowNumber;
    private String assetCode;
    private String assetName;
    private boolean success;
    private String errorMessage;
}