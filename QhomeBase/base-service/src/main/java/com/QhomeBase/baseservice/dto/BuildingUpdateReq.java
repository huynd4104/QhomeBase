package com.QhomeBase.baseservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BuildingUpdateReq(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 512) String address,
        Integer numberOfFloors
) {}