package com.QhomeBase.baseservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record PrimaryResidentProvisionRequest(
        @NotNull @Valid ResidentCreateDto resident,
        @Valid CreateResidentAccountDto account,
        String relation
) {}


