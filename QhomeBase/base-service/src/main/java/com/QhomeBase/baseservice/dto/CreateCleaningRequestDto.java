package com.QhomeBase.baseservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public record CreateCleaningRequestDto(
        @NotNull(message = "Unit ID is required")
        UUID unitId,

        @NotBlank(message = "Cleaning type is required")
        String cleaningType,

        @NotNull(message = "Cleaning date is required")
        LocalDate cleaningDate,

        @NotNull(message = "Start time is required")
        LocalTime startTime,

        @NotNull(message = "Duration is required")
        @DecimalMin(value = "0.5", message = "Duration must be greater than zero")
        BigDecimal durationHours,

        @NotBlank(message = "Location is required")
        String location,

        String note,

        @NotBlank(message = "Contact phone is required")
        String contactPhone,

        List<String> extraServices,

        String paymentMethod
) {
    public CreateCleaningRequestDto {
        extraServices = extraServices == null
                ? List.of()
                : extraServices.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .limit(10)
                .collect(Collectors.toCollection(ArrayList::new));
    }
}

