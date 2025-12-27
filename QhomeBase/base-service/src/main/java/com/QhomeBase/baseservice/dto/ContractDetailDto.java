package com.QhomeBase.baseservice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ContractDetailDto(
        UUID id,
        UUID unitId,
        String contractNumber,
        String contractType,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal monthlyRent,
        BigDecimal purchasePrice,
        String paymentMethod,
        String paymentTerms,
        LocalDate purchaseDate,
        String notes,
        String status,
        UUID createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        UUID updatedBy,
        List<ContractFileDto> files
) {}




