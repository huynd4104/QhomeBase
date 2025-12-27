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
public class ToggleServiceStatusRequest {

    @NotNull(message = "Active flag is required")
    private Boolean active;
}

