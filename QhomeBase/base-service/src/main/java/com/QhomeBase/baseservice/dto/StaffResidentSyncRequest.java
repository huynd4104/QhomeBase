package com.QhomeBase.baseservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record StaffResidentSyncRequest(
        @NotNull(message = "User ID is required")
        UUID userId,

        @Size(min = 3, max = 255, message = "Full name must be between 3 and 255 characters")
        String fullName,

        @Email(message = "Email must be valid")
        String email,

        @Size(max = 20, message = "Phone number must not exceed 20 characters")
        String phone
) {
}





