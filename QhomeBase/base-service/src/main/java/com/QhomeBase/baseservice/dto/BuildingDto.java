package com.QhomeBase.baseservice.dto;

import com.QhomeBase.baseservice.model.BuildingStatus;

import java.util.UUID;

public record BuildingDto(
                UUID id,
                String code,
                String name,
                String address,
                Integer floorsMax,
                Integer totalApartmentsAll,

                Integer totalApartmentsActive,
                BuildingStatus status,
                String createdAt,
                String createdBy) {
}