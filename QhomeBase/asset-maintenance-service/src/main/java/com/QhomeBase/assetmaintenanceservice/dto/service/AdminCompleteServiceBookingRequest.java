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
public class AdminCompleteServiceBookingRequest {

    @Size(max = 2000, message = "Completion note must not exceed 2000 characters")
    private String completionNote;
}

