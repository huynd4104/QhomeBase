package com.QhomeBase.baseservice.dto;

import jakarta.validation.constraints.NotBlank;

public record BuildingDeletionRejectReq(
        @NotBlank String note
) {}