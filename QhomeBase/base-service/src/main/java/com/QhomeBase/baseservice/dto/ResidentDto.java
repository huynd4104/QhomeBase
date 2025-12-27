package com.QhomeBase.baseservice.dto;

import com.QhomeBase.baseservice.model.ResidentStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ResidentDto(
        UUID id,
        String fullName,
        String phone,
        String email,
        String nationalId,
        LocalDate dob,
        ResidentStatus status,
        UUID userId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}


