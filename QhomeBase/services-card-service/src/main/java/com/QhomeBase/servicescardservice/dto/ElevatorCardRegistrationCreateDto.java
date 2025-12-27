package com.QhomeBase.servicescardservice.dto;

import java.util.UUID;

public record ElevatorCardRegistrationCreateDto(
        String apartmentNumber,
        String buildingName,
        String citizenId,
        String phoneNumber,
        String note,
        UUID unitId,
        UUID residentId,
        String requestType,
        String fullName
) {}

