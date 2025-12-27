package com.QhomeBase.baseservice.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ContractFileDto(
        UUID id,
        UUID contractId,
        String fileName,
        String originalFileName,
        String fileUrl,
        String proxyUrl,
        String contentType,
        Long fileSize,
        Boolean isPrimary,
        Integer displayOrder,
        UUID uploadedBy,
        OffsetDateTime uploadedAt
) {}

