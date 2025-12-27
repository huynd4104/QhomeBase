package com.QhomeBase.iamservice.dto;

import java.util.List;

public record StaffImportResponse(
        int totalRows,
        int successCount,
        int failureCount,
        List<StaffImportRowResult> rows
) {
}











