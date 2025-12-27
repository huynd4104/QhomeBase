package com.QhomeBase.assetmaintenanceservice.client.dto;

import java.util.UUID;

public record BuildingDto(
        UUID id,
        String code,
        String name,
        String address,
        Integer floorsMax,
        Integer totalApartmentsAll,
        Integer totalApartmentsActive
) {}










