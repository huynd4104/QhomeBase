package com.QhomeBase.datadocsservice.dto.imports;

import lombok.Builder;

import java.util.UUID;

@Builder
public record ContractImportRowResult(
        int rowNumber,
        boolean success,
        String contractNumber,       // null if failed
        UUID createdContractId,      // null if failed
        String message               // "OK" or error reason
) {}









