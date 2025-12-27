package com.QhomeBase.baseservice.dto;

import com.QhomeBase.baseservice.model.ResidentStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ResidentUpdateDto(
        @Size(max = 100, message = "Full name must not exceed 100 characters")
        String fullName,

        @Pattern(regexp = "^[0-9+\\-\\s()]+$", message = "Phone number format is invalid")
        @Size(max = 20, message = "Phone number must not exceed 20 characters")
        String phone,

        @Email(message = "Email format is invalid")
        @Size(max = 100, message = "Email must not exceed 100 characters")
        String email,

        @Size(max = 20, message = "National ID must not exceed 20 characters")
        String nationalId,

        LocalDate dob,

        ResidentStatus status
) {}


