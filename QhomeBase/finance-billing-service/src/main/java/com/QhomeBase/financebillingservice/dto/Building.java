package com.QhomeBase.financebillingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Building {
    private UUID id;
    private UUID tenantId;
    private String codeName;
    private String name;
    private String address;
    private Instant createdAt;
    private Instant updatedAt;
}
