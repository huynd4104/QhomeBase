package com.QhomeBase.assetmaintenanceservice.dto.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateServiceAvailabilityRequest {

    @NotEmpty(message = "Availability list must not be empty")
    @Valid
    private List<ServiceAvailabilityRequest> availabilities;
}

