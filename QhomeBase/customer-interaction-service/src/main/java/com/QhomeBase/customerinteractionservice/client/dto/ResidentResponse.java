package com.QhomeBase.customerinteractionservice.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ResidentResponse(
        UUID id,
        @JsonProperty("fullName") String fullName,
        String phone,
        String email,
        String nationalId,
        LocalDate dob,
        String status,
        UUID userId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}





