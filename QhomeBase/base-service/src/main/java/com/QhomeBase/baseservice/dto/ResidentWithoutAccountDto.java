package com.QhomeBase.baseservice.dto;

import com.QhomeBase.baseservice.model.ResidentStatus;

import java.time.LocalDate;
import java.util.UUID;

public record ResidentWithoutAccountDto(
        UUID id,
        String fullName,
        String phone,
        String email,
        String nationalId,
        LocalDate dob,
        ResidentStatus status,
        String relation,
        boolean isPrimary
) {}
