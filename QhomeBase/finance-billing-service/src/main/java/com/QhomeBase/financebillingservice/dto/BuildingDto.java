package com.QhomeBase.financebillingservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuildingDto {
    private UUID id;
    private UUID tenantId;
    @JsonProperty("codeName")
    private String code;
    private String name;
    private String address;
    private Instant createdAt;
    private Instant updatedAt;
}

