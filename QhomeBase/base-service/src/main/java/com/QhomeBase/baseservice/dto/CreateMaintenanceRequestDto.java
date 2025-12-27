package com.QhomeBase.baseservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public record CreateMaintenanceRequestDto(
        @NotNull(message = "Unit ID is required")
        UUID unitId,

        @NotBlank(message = "Category is required")
        String category,

        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be less than 200 characters")
        String title,

        @NotBlank(message = "Description is required")
        String description,

        List<String> attachments,

        @NotBlank(message = "Location is required")
        String location,

        @NotNull(message = "Preferred datetime is required")
        OffsetDateTime preferredDatetime,

        String contactName,

        String contactPhone,

        String note
) {
    public CreateMaintenanceRequestDto {
        attachments = attachments == null
                ? List.of()
                : attachments.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .limit(3)
                .collect(Collectors.toCollection(ArrayList::new));
    }
}

