package com.QhomeBase.baseservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleActivatedEvent {
    private UUID vehicleId;
    private UUID unitId;
    private UUID residentId;
    private String plateNo;
    private String vehicleKind;
    private OffsetDateTime activatedAt;
    private UUID approvedBy;
}