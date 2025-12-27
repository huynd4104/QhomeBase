package com.QhomeBase.baseservice.dto;

import com.QhomeBase.baseservice.model.ServiceType;
import com.QhomeBase.baseservice.model.ServiceUnit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ServiceCreateReq(
        @NotBlank String code,
        @NotBlank String name,
        String nameEn,
        @NotNull ServiceType type,
        @NotNull ServiceUnit unit,
        String unitLabel,
        Boolean billable,
        Boolean requiresMeter,
        String description
) {}

