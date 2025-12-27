package com.QhomeBase.baseservice.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record BuildingDeletionCreateReq(
        @NotNull UUID buildingId,
        String reason
) {}