package com.QhomeBase.baseservice.dto;

import com.QhomeBase.baseservice.model.VehicleKind;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record VehicleUpdateDto(
        UUID residentId,
        
        UUID unitId,
        
        @Size(max = 20, message = "Plate number must not exceed 20 characters")
        String plateNo,
        
        VehicleKind kind,
        
        @Size(max = 50, message = "Color must not exceed 50 characters")
        String color,
        
        Boolean active
) {}


