package com.QhomeBase.assetmaintenanceservice.dto.service;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelServiceBookingRequest {

    @Size(max = 2000, message = "Cancellation reason must not exceed 2000 characters")
    private String reason;
}

