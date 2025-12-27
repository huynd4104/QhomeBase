package com.QhomeBase.marketplaceservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResidentInfoResponse {
    private UUID residentId;
    private String name;
    private String avatarUrl;
    private String unitNumber;
    private UUID buildingId;
    private String buildingName; // Building name (e.g., "Tòa A", "Tòa B")
}

