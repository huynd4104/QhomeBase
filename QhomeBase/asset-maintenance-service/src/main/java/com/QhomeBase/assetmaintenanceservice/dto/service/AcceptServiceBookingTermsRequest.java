package com.QhomeBase.assetmaintenanceservice.dto.service;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcceptServiceBookingTermsRequest {

    @NotNull(message = "Terms accepted flag is required")
    private Boolean accepted;
}

