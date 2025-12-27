package com.QhomeBase.datadocsservice.dto.imports;

import lombok.Builder;

import java.util.List;

@Builder
public record ContractImportResponse(
        int totalRows,
        int successCount,
        int failureCount,
        List<ContractImportRowResult> rows
) {}









