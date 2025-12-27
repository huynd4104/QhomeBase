package com.QhomeBase.servicescardservice.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import java.util.List;

public record RegisterServiceRequestCreateDto(
        @NotBlank(message = "Service type is required")
        String serviceType,
        
        String requestType,
        
        String note,
        
        UUID unitId,

        String vehicleType,
        
        String licensePlate,
        
        String vehicleBrand,
        
        String vehicleColor,
        
        String apartmentNumber,
        
        String buildingName,
        
        List<String> imageUrls,
        
        UUID originalCardId
) {}

