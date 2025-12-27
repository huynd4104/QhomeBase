package com.QhomeBase.baseservice.dto;

import jakarta.validation.constraints.NotBlank;

public record BuildingDeletionApproveReq(
        @NotBlank String note
) {}